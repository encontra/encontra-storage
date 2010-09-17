package pt.inevo.encontra.storage.test;

import pt.inevo.encontra.storage.IEntity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.awt.image.BufferedImage;

@Entity
public class MyObject implements IEntity<Long> {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Transient
    private BufferedImage image;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id=id;
    }
}
