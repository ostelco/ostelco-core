package com.telenordigital.ocsgw.utils;

import org.apache.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.validation.AvpRepresentation;
import org.jdiameter.api.validation.Dictionary;
import org.jdiameter.common.impl.validation.DictionaryImpl;

public class DiameterUtilities {

    private static final Logger logger = Logger.getLogger(DiameterUtilities.class);

    private static final Dictionary AVP_DICTIONARY = DictionaryImpl.INSTANCE;

    public static void printAvps(AvpSet avps) {
        printAvps(avps, "");
    }

    private static void printAvps(AvpSet avps, String indentation) {
        for (Avp avp : avps) {
            AvpRepresentation avpRep = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId());
            Object avpValue;
            boolean isGrouped = false;

            try {
                String avpType = AVP_DICTIONARY.getAvp(avp.getCode(), avp.getVendorId()).getType();

                switch (avpType) {
                    case "Integer32":
                    case "AppId":
                        avpValue = avp.getInteger32();
                        break;
                    case "Unsigned32":
                    case "VendorId":
                        avpValue = avp.getUnsigned32();
                        break;
                    case "Float64":
                        avpValue = avp.getFloat64();
                        break;
                    case "Integer64":
                        avpValue = avp.getInteger64();
                        break;
                    case "Time":
                        avpValue = avp.getTime();
                        break;
                    case "Unsigned64":
                        avpValue = avp.getUnsigned64();
                        break;
                    case "Grouped":
                        avpValue = "<Grouped>";
                        isGrouped = true;
                        break;
                    default:
                        avpValue = avp.getUTF8String().replaceAll("\r", "").replaceAll("\n", "");
                        break;
                }
            } catch (Exception ignore) {
                try {
                    avpValue = avp.getUTF8String().replaceAll("\r", "").replaceAll("\n", "");
                } catch (AvpDataException e) {
                    avpValue = avp.toString();
                }
            }

            StringBuilder avpLine = new StringBuilder(indentation + avp.getCode() + ": " + avpRep.getName());
            while (avpLine.length() < 50) {
                avpLine.append(avpLine.length() % 2 == 0 ? "." : " ");
            }
            avpLine.append(avpValue);

            logger.info(avpLine.toString());

            if (isGrouped) {
                try {
                    printAvps(avp.getGrouped(), indentation + "  ");
                } catch (AvpDataException e) {
                    // Failed to ungroup... ignore then...
                }
            }
        }
    }
}
