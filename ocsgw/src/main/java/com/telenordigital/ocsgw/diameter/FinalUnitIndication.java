package com.telenordigital.ocsgw.diameter;

import java.util.LinkedList;

public class FinalUnitIndication {


    private int finalUnitAction = FinalUnitAction.TERMINATE;
    private LinkedList<IPFilterRule> restrictionFilterRule = new LinkedList<>();
    private LinkedList<String> filterId = new LinkedList<>();
    private RedirectServer redirectServer = new RedirectServer();

    public int getFinalUnitAction() {
        return finalUnitAction;
    }

    public void setFinalUnitAction(int finalUnitAction) {
        this.finalUnitAction = finalUnitAction;
    }

    public LinkedList<IPFilterRule> getRestrictionFilterRule() {
        return restrictionFilterRule;
    }

    public void setRestrictionFilterRule(LinkedList<IPFilterRule> restrictionFilterRule) {
        this.restrictionFilterRule = restrictionFilterRule;
    }

    public LinkedList<String> getFilterId() {
        return filterId;
    }

    public void setFilterId(LinkedList<String> filterId) {
        this.filterId = filterId;
    }

    public RedirectServer getRedirectServer() {
        return redirectServer;
    }

    public void setRedirectServer(RedirectServer redirectServer) {
        this.redirectServer = redirectServer;
    }
}
