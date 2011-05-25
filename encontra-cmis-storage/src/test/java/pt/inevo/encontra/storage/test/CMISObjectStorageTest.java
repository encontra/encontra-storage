package pt.inevo.encontra.storage.test;

import junit.framework.TestCase;
import pt.inevo.encontra.storage.CMISObjectStorage;

import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.Test;

public class CMISObjectStorageTest extends TestCase {

    private CMISObjectStorage<MyCMISObject> storage;

    public class MyObjectStorage extends CMISObjectStorage<MyCMISObject> {}

    public CMISObjectStorageTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storage=new MyObjectStorage();
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
    }
}
