
package com.photostudio.pro;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.JSObject;

import java.io.OutputStream;

@CapacitorPlugin(name = "GallerySaver")
public class GallerySaverPlugin extends Plugin {

    @PluginMethod
    public void save(PluginCall call) {
        String data = call.getString("data");
        String fileName = call.getString("fileName", "PhotoStudio.jpg");
        String mimeType = call.getString("mimeType", "image/jpeg");

        if (data == null || data.isEmpty()) {
            call.reject("Imagem vazia.");
            return;
        }

        Uri imageUri = null;

        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Photo Studio Pro"
                );
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            imageUri = getContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            );

            if (imageUri == null) {
                throw new Exception("Não foi possível criar a imagem.");
            }

            try (OutputStream output =
                getContext().getContentResolver().openOutputStream(imageUri)) {

                if (output == null) {
                    throw new Exception("Falha ao abrir arquivo.");
                }

                output.write(bytes);
                output.flush();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues done = new ContentValues();
                done.put(MediaStore.Images.Media.IS_PENDING, 0);

                getContext().getContentResolver().update(
                    imageUri, done, null, null
                );
            }

            JSObject result = new JSObject();
            result.put("uri", imageUri.toString());

            call.resolve(result);

        } catch (Exception e) {

            if (imageUri != null) {
                getContext().getContentResolver().delete(
                    imageUri, null, null
                );
            }

            call.reject("Erro ao salvar na galeria: " + e.getMessage(), e);
        }
    }
}
