package pt.inevo.encontra.storage;

import java.util.Map;

/**
 * CmisObject.
 * @author rspd
 */
public interface CmisObject extends IEntity<String> {

    public Map<String, Object> getProperties();
    public byte [] getContent();
    public void setContent(byte [] content);
}
