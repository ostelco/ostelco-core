package org.ostelco.diameter.model;

public class RequestType {
    public static final int INITIAL_REQUEST = 1;
    public static final int UPDATE_REQUEST = 2;
    public static final int TERMINATION_REQUEST = 3;
    public static final int EVENT_REQUEST = 4;

    public static String getTypeAsString(int type) {

        String typeString;
        switch (type) {
            case RequestType.INITIAL_REQUEST :
                typeString = "INITIAL";
                break;
            case RequestType.UPDATE_REQUEST:
                typeString = "UPDATE";
                break;
            case RequestType.TERMINATION_REQUEST:
                typeString = "TERMINATE";
                break;
            case RequestType.EVENT_REQUEST:
                typeString = "EVENT";
                break;
            default:
                typeString = Integer.toString(type);
                break;
        }
        return typeString;
    }

}
