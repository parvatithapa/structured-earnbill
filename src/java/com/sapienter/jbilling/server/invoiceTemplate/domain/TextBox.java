package com.sapienter.jbilling.server.invoiceTemplate.domain;

import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;

import static net.sf.jasperreports.engine.type.EvaluationTimeEnum.AUTO;
import static net.sf.jasperreports.engine.type.HorizontalAlignEnum.LEFT;

/**
 * @author elmot
 */
public class TextBox extends Text {

    private int roundCornerRadius;

    private EvaluationTimeEnum evaluationTime;

    public int getRoundCornerRadius() {
        return roundCornerRadius;
    }

    public void setRoundCornerRadius(int roundCornerRadius) {
        this.roundCornerRadius = roundCornerRadius;
    }

    public EvaluationTimeEnum getEvaluationTime() {
        return evaluationTime == null ? AUTO : evaluationTime;
    }

    public void setEvaluationTime(EvaluationTimeEnum evaluationTime) {
        this.evaluationTime = evaluationTime;
    }

    @Override
    public void visit(Visitor visitor) {
        visitor.accept(this);
    }
}
