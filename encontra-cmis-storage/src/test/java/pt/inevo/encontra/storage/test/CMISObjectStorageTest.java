package pt.inevo.encontra.storage.test;

import junit.framework.TestCase;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import pt.inevo.encontra.query.criteria.StorageCriteria;
import pt.inevo.encontra.storage.CMISObjectStorage;

import javax.naming.Binding;
import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.util.*;

import org.junit.Test;

public class CMISObjectStorageTest extends TestCase {

    private CMISObjectStorage<MyCMISObject> storage;

    public class MyObjectStorage extends CMISObjectStorage<MyCMISObject> {

        MyObjectStorage(Map<String, String> parameters) {
            super(parameters);
        }
    }

    public CMISObjectStorageTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Properties properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/config.properties"));

        Map<String, String> parameter = new HashMap<String, String>();
        Enumeration e = properties.propertyNames();
        while (e.hasMoreElements()) {
            String propertyName = e.nextElement().toString();
            parameter.put(propertyName, properties.getProperty(propertyName));
        }

        storage=new MyObjectStorage(parameter);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

    }

    @Test
    public void testStored() {
        int max=10;
        int test=new Random().nextInt(max);
        String id=null;
        MyCMISObject obj;
        for(int i=0;i<max;i++){
            obj=new MyCMISObject();
            obj.setName("A Test Object");
            obj.setDescription("Object" + i + " description");
            obj.setImage(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB));
            obj=storage.save(obj);
            if(i==test)
                id=obj.getId();
        }

        obj=storage.get(id);
        assertNotNull(obj);
        assertEquals(id,obj.getId());

        obj.dumpObject();

        StorageCriteria criteria = new StorageCriteria("cmis:contentStreamFileName like '%Object%'");
        boolean valid = storage.validate(obj.getId(), criteria);
        assertTrue(valid);
    }
}
