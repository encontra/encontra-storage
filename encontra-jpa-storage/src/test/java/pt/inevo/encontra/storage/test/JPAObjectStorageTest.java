package pt.inevo.encontra.storage.test;

import junit.framework.TestCase;
import pt.inevo.encontra.storage.JPAObjectStorage;

import javax.persistence.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.Test;

public class JPAObjectStorageTest extends TestCase {

    EntityManagerFactory emf;
    EntityManager em;
    JPAObjectStorage<Long,MyObject> storage;

    public class MyObjectStorage extends JPAObjectStorage<Long,MyObject> {}

    public JPAObjectStorageTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Use persistence.xml configuration
        emf = Persistence.createEntityManagerFactory("manager");
        em = emf.createEntityManager(); // Retrieve an application managed entity manager

        storage=new MyObjectStorage();
        storage.setEntityManager(em);

    }

    @Override
    protected void tearDown() throws Exception {
        em.close();

        emf.close(); //close at application end
        super.tearDown();

    }

    @Test
    public void testVectorize() {
        MyObject obj=new MyObject();
        obj.setName("A Test Object");
        obj.setImage(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));

        MyObject stored=storage.save(obj);

        assertNotNull(stored.getId());

        stored=storage.get(stored.getId());
        System.out.println("Object id:" + stored.getId());
        assertNull(stored.getImage());
    }

    @Test
    public void testStorage() {
        int max=10;
        int test=new Random().nextInt(max);
        Long id=null;
        MyObject obj;
        for(int i=0;i<max;i++){
            obj=new MyObject();
            obj.setName("A Test Object");
            obj.setImage(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB));
            obj=storage.save(obj);
            if(i==test)
                id=obj.getId();

        }

        obj=storage.get(id);
        assertNotNull(obj);
        assertEquals(id,obj.getId());
    }
}
