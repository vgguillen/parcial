package WebServices;

import org.json.JSONException;

public interface Asynchtask {
    /**
     * Retornamos los datos de la API
     * @param result
     */
    void processFinish(String result) throws JSONException;

}
