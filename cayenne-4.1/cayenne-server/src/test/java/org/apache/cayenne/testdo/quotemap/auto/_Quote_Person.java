package org.apache.cayenne.testdo.quotemap.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.quotemap.QuoteAdress;

/**
 * Class _Quote_Person was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Quote_Person extends BaseDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "id";

    public static final Property<Date> D_ATE = Property.create("dAte", Date.class);
    public static final Property<String> F_ULL_NAME = Property.create("fULL_name", String.class);
    public static final Property<String> GROUP = Property.create("group", String.class);
    public static final Property<String> NAME = Property.create("name", String.class);
    public static final Property<Integer> SALARY = Property.create("salary", Integer.class);
    public static final Property<QuoteAdress> ADDRESS_REL = Property.create("address_Rel", QuoteAdress.class);

    protected Date dAte;
    protected String fULL_name;
    protected String group;
    protected String name;
    protected Integer salary;

    protected Object address_Rel;

    public void setDAte(Date dAte) {
        beforePropertyWrite("dAte", this.dAte, dAte);
        this.dAte = dAte;
    }

    public Date getDAte() {
        beforePropertyRead("dAte");
        return this.dAte;
    }

    public void setFULL_name(String fULL_name) {
        beforePropertyWrite("fULL_name", this.fULL_name, fULL_name);
        this.fULL_name = fULL_name;
    }

    public String getFULL_name() {
        beforePropertyRead("fULL_name");
        return this.fULL_name;
    }

    public void setGroup(String group) {
        beforePropertyWrite("group", this.group, group);
        this.group = group;
    }

    public String getGroup() {
        beforePropertyRead("group");
        return this.group;
    }

    public void setName(String name) {
        beforePropertyWrite("name", this.name, name);
        this.name = name;
    }

    public String getName() {
        beforePropertyRead("name");
        return this.name;
    }

    public void setSalary(Integer salary) {
        beforePropertyWrite("salary", this.salary, salary);
        this.salary = salary;
    }

    public Integer getSalary() {
        beforePropertyRead("salary");
        return this.salary;
    }

    public void setAddress_Rel(QuoteAdress address_Rel) {
        setToOneTarget("address_Rel", address_Rel, true);
    }

    public QuoteAdress getAddress_Rel() {
        return (QuoteAdress)readProperty("address_Rel");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "dAte":
                return this.dAte;
            case "fULL_name":
                return this.fULL_name;
            case "group":
                return this.group;
            case "name":
                return this.name;
            case "salary":
                return this.salary;
            case "address_Rel":
                return this.address_Rel;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "dAte":
                this.dAte = (Date)val;
                break;
            case "fULL_name":
                this.fULL_name = (String)val;
                break;
            case "group":
                this.group = (String)val;
                break;
            case "name":
                this.name = (String)val;
                break;
            case "salary":
                this.salary = (Integer)val;
                break;
            case "address_Rel":
                this.address_Rel = val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.dAte);
        out.writeObject(this.fULL_name);
        out.writeObject(this.group);
        out.writeObject(this.name);
        out.writeObject(this.salary);
        out.writeObject(this.address_Rel);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.dAte = (Date)in.readObject();
        this.fULL_name = (String)in.readObject();
        this.group = (String)in.readObject();
        this.name = (String)in.readObject();
        this.salary = (Integer)in.readObject();
        this.address_Rel = in.readObject();
    }

}
