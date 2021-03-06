package eu.peppol.start.identifier;

import java.io.Serializable;

/**
 * @author Steinar Overbeck Cook
 *         <p/>
 *         Created by
 *         User: steinar
 *         Date: 04.12.11
 *         Time: 18:46
 */
public class ChannelId implements Serializable {

    String value;

    public ChannelId(String channelId) {
        if (channelId == null) {
            value="";
        } else
            this.value = channelId;
    }

    public String stringValue() {
        return toString();
    }
    
    @Override
    public String toString() {
        return value ;
    }
}
