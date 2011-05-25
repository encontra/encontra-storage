package pt.inevo.encontra.storage.test;

import pt.inevo.encontra.storage.CmisObject;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MyCMISObject implements CmisObject {

    /*Internal object properties*/
    private String id = "";
    private String name = "";
    private String description = "";
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
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("cmis:name", name);
        properties.put("cmis:contentStreamFileName", description);
        return properties;
    }

    @Override
    public byte[] getContent() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(output);
            stream.writeUTF(id);
            stream.writeUTF(name);
            stream.writeUTF(description);
            stream.flush();

            byte [] content = output.toByteArray();
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[1];
    }

    @Override
    public void setContent(byte [] content) {
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(content);
            ObjectInputStream stream = new ObjectInputStream(input);
            id = stream.readUTF();
            name = stream.readUTF();
            description = stream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dumpObject() {
        System.out.println("Id: " + id);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);
    }
}
