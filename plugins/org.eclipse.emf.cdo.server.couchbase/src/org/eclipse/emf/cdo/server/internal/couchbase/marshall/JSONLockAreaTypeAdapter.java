package org.eclipse.emf.cdo.server.internal.couchbase.marshall;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JSONLockAreaTypeAdapter implements JsonDeserializer<JSONLockArea>,
		JsonSerializer<JSONLockArea> {

	private static final String ID = "id";
	private static final String USER_ID = "user_id";
	private static final String BRANCH_ID = "branch_id";
	private static final String TIMESTAMP = "timestamp";
	private static final String READONLY = "readonly";
	private static final String ENTRY_CDOID = "entry_cdoid_";
	private static final String ENTRY_GRADE = "entry_grade_";

	public JsonElement serialize(JSONLockArea arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		obj.addProperty(ID, arg0.getId());
		obj.addProperty(USER_ID, arg0.getUserID());
		obj.addProperty(BRANCH_ID, arg0.getBranchID());
		obj.addProperty(TIMESTAMP, arg0.getTimestamp());
		obj.addProperty(READONLY, arg0.isReadOnly());
		int count = 0;
		for (JSONLockEntry entry : arg0.getLockEntries()) {
			obj.addProperty(getEntryCDOIDKey(count), entry.getCdoID());
			obj.addProperty(getEntryGradeKey(count), entry.getLockGrade());
			count++;
		}
		return obj;
	}

	public JSONLockArea deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
		JSONLockArea jsonLockArea = new JSONLockArea();
		JsonObject json = (JsonObject) arg0;
		jsonLockArea.setId(json.get(ID).getAsString());
		jsonLockArea.setUserID(json.get(USER_ID).getAsString());
		jsonLockArea.setBranchID(json.get(BRANCH_ID).getAsInt());
		jsonLockArea.setTimestamp(json.get(TIMESTAMP).getAsLong());
		jsonLockArea.setReadOnly(json.get(READONLY).getAsBoolean());
		for (int i = 0; true; i++) {
			if (json.get(getEntryCDOIDKey(i)) != null) {
				jsonLockArea.getLockEntries().add(new JSONLockEntry(json.get(getEntryCDOIDKey(i))
				.getAsLong(), json.get(getEntryGradeKey(i)).getAsInt()));
			} else {
				break;
			}
		}
		return jsonLockArea;
	}

	private String getEntryCDOIDKey(int count) {
		return ENTRY_CDOID + count;
	}

	private String getEntryGradeKey(int count) {
		return ENTRY_GRADE + count;
	}
}
