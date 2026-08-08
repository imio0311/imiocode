package io.imiocode.skill.install;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

/** 手动校验重定向且支持取消的 Java HTTP 传输。 */
public final class JdkSkillRemoteTransport implements SkillRemoteTransport {
    private static final int MAX_REDIRECTS = 3;
    private final HttpClient client;
    private final SkillInstallConfig config;
    private final AtomicReference<CompletableFuture<?>> active = new AtomicReference<>();
    private volatile boolean cancelled;

    public JdkSkillRemoteTransport(SkillInstallConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    public JdkSkillRemoteTransport(SkillInstallConfig config, HttpClient client) {
        this.config = java.util.Objects.requireNonNull(config, "config");
        this.client = java.util.Objects.requireNonNull(client, "client");
    }

    @Override
    public RemoteResponse get(URI initial, SkillDownloadBudget budget) {
        if (!budget.cancelled()) cancelled = false;
        URI current = initial;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            ensureActive(budget);
            RemoteSkillLocator.validatePublicUri(current, config.allowedHosts());
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(config.timeout())
                    .header("Accept", "application/vnd.github+json, text/plain;q=0.9, */*;q=0.1")
                    .header("User-Agent", "ImioCode-SkillInstaller/1")
                    .GET().build();
            CompletableFuture<HttpResponse<InputStream>> future = client.sendAsync(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            active.set(future);
            HttpResponse<InputStream> response;
            try {
                response = future.join();
            } catch (CancellationException exception) {
                throw new SkillInstallException("Skill 安装已取消");
            } catch (CompletionException exception) {
                if (cancelled || budget.cancelled()) throw new SkillInstallException("Skill 安装已取消");
                Throwable cause = exception.getCause();
                if (cause instanceof java.net.http.HttpTimeoutException) {
                    throw new SkillInstallException("远程 Skill 下载超时");
                }
                if (cause instanceof java.net.ConnectException) {
                    throw new SkillInstallException("无法连接远程 Skill 服务");
                }
                throw new SkillInstallException("无法下载远程 Skill");
            } finally {
                active.compareAndSet(future, null);
            }
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                closeQuietly(response.body());
                if (redirects == MAX_REDIRECTS) throw new SkillInstallException("Skill 下载重定向次数过多");
                String location = response.headers().firstValue("Location")
                        .orElseThrow(() -> new SkillInstallException("Skill 下载重定向缺少目标"));
                current = current.resolve(location);
                RemoteSkillLocator.validatePublicUri(current, config.allowedHosts());
                continue;
            }
            byte[] body = readBounded(response, budget);
            if (status < 200 || status >= 300) {
                throw new SkillInstallException("远程 Skill 下载失败（HTTP " + status + "）");
            }
            return new RemoteResponse(current, status, response.headers().map(), body);
        }
        throw new SkillInstallException("Skill 下载重定向次数过多");
    }

    private byte[] readBounded(HttpResponse<InputStream> response, SkillDownloadBudget budget) {
        long remaining = budget.remainingBytes();
        Optional<String> declared = response.headers().firstValue("Content-Length");
        if (declared.isPresent()) {
            try {
                if (Long.parseLong(declared.get()) > remaining) {
                    closeQuietly(response.body());
                    throw new SkillInstallException("远程 Skill 总大小超过限制");
                }
            } catch (NumberFormatException ignored) {
                // 非法 Content-Length 仍由实际读取预算兜底。
            }
        }
        try (InputStream input = response.body(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long read = 0;
            while (true) {
                ensureActive(budget);
                int count = input.read(buffer);
                if (count < 0) break;
                read += count;
                if (read > remaining) throw new SkillInstallException("远程 Skill 总大小超过限制");
                output.write(buffer, 0, count);
            }
            budget.consumeResponseBytes(read);
            return output.toByteArray();
        } catch (IOException exception) {
            if (cancelled || budget.cancelled()) throw new SkillInstallException("Skill 安装已取消");
            throw new SkillInstallException("读取远程 Skill 失败");
        }
    }

    private void ensureActive(SkillDownloadBudget budget) {
        if (cancelled) budget.cancel();
        budget.checkCancelled();
    }

    @Override
    public void cancel() {
        cancelled = true;
        CompletableFuture<?> future = active.getAndSet(null);
        if (future != null) future.cancel(true);
    }

    private static void closeQuietly(InputStream stream) {
        try { stream.close(); } catch (IOException ignored) { }
    }
}
