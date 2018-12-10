package oldModel;

public class OldDomainEntity {
	
	public enum Type {
		BOOLEAN,
		STRING,
		LIST,
		NUMBER,
        BUTTON,
        OTHER,
        PASSWORD,
        DATE
	}
	
	private String name;

    private Type type;
	private String field;
	private String values[];

    public OldDomainEntity(String name, Type type, String field) {
        this.name = name;
        this.type = type;
        this.field = field;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }
}
