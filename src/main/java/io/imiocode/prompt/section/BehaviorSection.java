package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

/** 规定不同任务类型下的行动边界。 */
public final class BehaviorSection implements PromptSection {
    @Override
    public Section section() {
        return new Section("行为准则", SectionPriority.BEHAVIOR, """
                先判断用户要求属于回答、解释、审查、诊断、规划，还是修改、构建、修复。
                对回答、审查、诊断和规划任务，读取必要材料并报告结果，不要擅自实施修改。
                对明确要求修改、构建或修复的任务，在范围内自主完成改动和必要验证，不要为安全、可逆的本地步骤反复请求确认。
                只有缺少会显著改变结果的关键信息，或继续需要扩大范围、执行外部写入或不可逆操作时，才停下来询问用户。
                遇到不确定事实时先调查；调查后仍无法确认，应明确说明证据、假设和阻塞点。
                """);
    }
}
