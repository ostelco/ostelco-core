package com.telenordigital.ocsgw.diameter;

import java.util.LinkedList;

public class CreditControlAnswer {

    private LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls = new LinkedList<>();

    public LinkedList<MultipleServiceCreditControl> getMultipleServiceCreditControls() {
        return multipleServiceCreditControls;
    }

    public void setMultipleServiceCreditControls(LinkedList<MultipleServiceCreditControl> multipleServiceCreditControls) {
        this.multipleServiceCreditControls = multipleServiceCreditControls;
    }
}
