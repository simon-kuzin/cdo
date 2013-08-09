package org.eclipse.emf.cdo.server.internal.couchbase.marshall;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.emf.cdo.common.model.CDOModelUtil;
import org.eclipse.emf.cdo.server.IRepository;
import org.eclipse.emf.cdo.server.IStore;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Gson is not able to properly serialize JSONRevision, so an adapter 
 * to manually serialize it is needed.
 *   
 * @author vroldan
 *
 */
public class JSONRevisionTypeAdapter implements JsonDeserializer<JSONRevision>,
												 JsonSerializer<JSONRevision> {
	
	private static final String FEATURE = "feature_";

	private static final String CONTAINER_ID = "containerId";

	private static final String VERSION = "version";

	private static final String TIMESTAMP = "timestamp";

	private static final String REVISED = "revised";

	private static final String RESOURCE_ID = "resourceId";

	private static final String ID = "id";

	private static final String CONTAINING_FEATURE_ID = "containingFeatureID";

	private static final String PACKAGE_URI = "packageURI";

	private static final String CLASS_NAME = "className";

	private IStore store;
	
	private static final String EMPTY_LIST = new String("$_empty_list");
	
	public JSONRevisionTypeAdapter(IStore store) {
		this.store = store;
	}	
	
	public JsonElement serialize(JSONRevision arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		obj.addProperty(CLASS_NAME, arg0.getClassName());
		obj.addProperty(PACKAGE_URI, arg0.getPackageURI());
		obj.addProperty(CONTAINING_FEATURE_ID, arg0.getContainingFeatureID());
		obj.addProperty(ID, arg0.getID());
		obj.addProperty(RESOURCE_ID, arg0.getResourceID());
		obj.addProperty(REVISED, arg0.getRevised());
		obj.addProperty(TIMESTAMP, arg0.getTimeStamp());
		obj.addProperty(VERSION, arg0.getVersion());
		obj.addProperty(CONTAINER_ID, arg0.getContainerID().toString());
		int count = 0;
		for (Object value : arg0.getValues()) {
			if (value == null) {
				obj.addProperty(getFeatureValueID(count), (String)null);
			} else if (value instanceof List<?>) {
				int nestedCount = 0;
				for (Object nestedValue : (List<?>)value) {
					obj.addProperty(getNestedFeatureValueID(count, nestedCount), getSerializableValue(nestedValue));
					nestedCount++;
				}
				if (nestedCount == 0) {
					obj.addProperty(getFeatureValueID(count), EMPTY_LIST);
				}					
			} else {
				if (value.getClass().isArray()) {
					  int length = Array.getLength(value);
					    for (int i = 0; i < length; i ++) {
					        Object arrayElement = Array.get(value, i);
							obj.addProperty(getNestedFeatureValueID(count, i), getSerializableValue(arrayElement));
					    }
				} else {
					obj.addProperty(getFeatureValueID(count), getSerializableValue(value));
				}
			}
			count++;
		}
		
		return obj;
	}

	private String getSerializableValue(Object value) {
		if (value == null || JSONRevision.isNull(value)) {
			return (String)null;
		} else if (value instanceof Date) {
			return String.valueOf(((Date)value).getTime());
		}
		return value.toString();
	}

	public JSONRevision deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
		JSONRevision rev = new JSONRevision();
		JsonObject json = (JsonObject)arg0;
		rev.setClassName(json.get(CLASS_NAME).getAsString());
		rev.setPackageURI(json.get(PACKAGE_URI).getAsString());
		rev.setContainingFeatureID(json.get(CONTAINING_FEATURE_ID).getAsInt());
		rev.setID(json.get(ID).getAsLong());
		rev.setResourceID(json.get(RESOURCE_ID).getAsLong());
		rev.setTimeStamp(json.get(TIMESTAMP).getAsLong());
		rev.setVersion(json.get(VERSION).getAsInt());
		rev.setContainerID(json.get(CONTAINER_ID).getAsString());
	
	    IRepository repository = store.getRepository();
	    String nsURI = rev.getPackageURI();
	    String className = rev.getClassName();
	    EPackage ePackage = repository.getPackageRegistry().getEPackage(nsURI);
	    EClass eClass = (EClass)ePackage.getEClassifier(className);
	    
	    int count = 0;
	    EStructuralFeature[] features = CDOModelUtil.getClassInfo(eClass).getAllPersistentFeatures();
	    List<Object> values = new ArrayList<Object>(features.length);
	    for (EStructuralFeature feature : features) {
	    	if (!feature.isDerived()) {
		    	if (feature.isMany()) {
		    		int nestedCount = 0;
		    		List<Object> nestedList = new ArrayList<Object>();
		    		JsonElement nestedValue = null;
		    		while (json.has(getNestedFeatureValueID(count, nestedCount))) {
		    			nestedValue = json.get(getNestedFeatureValueID(count, nestedCount));
		    			nestedList.add(getTypedElement(feature.getEType(), nestedValue, count));
		    			nestedCount++;
		    		};
		    		values.add(nestedList);
		    	} else {
		    		if (isByteArray(feature.getEType())) {
		    			values.add(getByteArray(json, count));
		    		}
		    		else {
		    			values.add(getTypedElement(feature.getEType(), json.get(getFeatureValueID(count)), count));
		    		}
		    	}
		    	count++;
	    	}
	    }
	    rev.setValues(values);
		return rev;
	}

	private String getFeatureValueID(int count) {
		return FEATURE + count;
	}
	
	private String getNestedFeatureValueID(int count, int nestedCount) {
		return FEATURE + count + nestedCount;
	}

	private boolean isByteArray(EClassifier eType) {
		return eType == EcorePackage.eINSTANCE.getEByteArray(); 
	}

	private Object getByteArray(JsonObject json, int count) {
		List<String> arrayList = getArrayValues(json, count);
		byte[] byteArray = new byte[arrayList.size()];
		for (int i = 0; i < arrayList.size(); i++) {
			byteArray[i] = Byte.valueOf(arrayList.get(i));
		}
		return byteArray;
	}

	private List<String> getArrayValues(JsonObject json, int count) {
		int nestedCount = 0;
		List<String> result = new ArrayList<String>();
		while (json.has(getNestedFeatureValueID(count, nestedCount))) {
			String nestedValue = json.get(getNestedFeatureValueID(count, nestedCount)).getAsString();
			result.add(nestedValue);
			nestedCount++;
		};
		return result;
	}

	private Object getTypedElement(EClassifier eType, JsonElement jsonElement, int currentCount) {
		if (eType instanceof EClass) {
			if (jsonElement == null || jsonElement instanceof JsonNull) {
				return JSONRevision.getCDOID(jsonElement);
			} else {
				return JSONRevision.getCDOID(jsonElement.toString());
			}
		} else if (eType instanceof EEnum) {
			if (jsonElement == null || jsonElement instanceof JsonNull) {
				return null;
			}
			return jsonElement.getAsInt();
		} else if (eType instanceof EDataType) {
			if (jsonElement == null || jsonElement instanceof JsonNull) {
				return null;
			}
			EDataType eDataType = (EDataType)eType;
			if (eDataType == EcorePackage.eINSTANCE.getEString()) {
				return jsonElement.getAsString();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEBigDecimal()) {
				return jsonElement.getAsBigDecimal();
			}   
			else if (eDataType == EcorePackage.eINSTANCE.getEBigInteger()) {
				return jsonElement.getAsBigInteger();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEBoolean()) {
				return jsonElement.getAsBoolean();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEByte()) {
				return jsonElement.getAsByte();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEChar()) {
				return jsonElement.getAsCharacter();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEDate()) {
				return new Date(jsonElement.getAsLong());
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEDouble()) {
				return jsonElement.getAsDouble();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEFloat()) {
				return jsonElement.getAsFloat();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getELong()) {
				return jsonElement.getAsLong();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEInt()) {
				return jsonElement.getAsInt();
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEIntegerObject()) {
				return new Integer(jsonElement.getAsInt());
			}
			else if (eDataType == EcorePackage.eINSTANCE.getEShort()) {
				return jsonElement.getAsShort();
			}
			return jsonElement.getAsString(); // for custom types
		}
		
		return null;
	}

}
