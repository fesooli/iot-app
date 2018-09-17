package br.com.iot.securityhouse;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.squareup.picasso.Picasso;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.File;
import java.nio.file.Path;

public class MainActivity extends AppCompatActivity {

    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + "/image.png";

    ImageView ivResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivResult = findViewById(R.id.ivResult);

        mqtt();
    }

    private void mqtt() {
        final String topic = "iot/test";
        final int qos = 1;
        try {
            String clientId = MqttClient.generateClientId();
            final MqttAndroidClient client =
                    new MqttAndroidClient(this.getApplicationContext(), "tcp://broker.hivemq.com:1883",
                            clientId);
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        IMqttToken subscribe = client.subscribe(topic, qos);
                        subscribe.setActionCallback(new IMqttActionListener() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {

                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {

                }

                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if(message.getPayload() != null) {
                        String result = new String(message.getPayload());
                        if(result.equalsIgnoreCase("enviar imagem"))
                            downloadWithTransferUtility();
                    } else {
                        Log.d("STATUS", "NÃO TEM MENSAGEM MQTT");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void downloadWithTransferUtility() {

        ClientConfiguration config = new ClientConfiguration();
        config.setConnectionTimeout(60000);
        config.setSocketTimeout(60000);

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(new AWSCredentialsProvider() {
                            @Override
                            public AWSCredentials getCredentials() {
                                return new AWSCredentials() {
                                    @Override
                                    public String getAWSAccessKeyId() {
                                        return ""; // access key
                                    }

                                    @Override
                                    public String getAWSSecretKey() {
                                        return ""; //secret key
                                    }
                                };
                            }

                            @Override
                            public void refresh() {

                            }
                        }, config)).build();

        TransferObserver downloadObserver = null;

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v("PERMISSION","Permission is granted");
            downloadObserver =
                    transferUtility.download(
                            "iot-image",
                            "image.png",
                            new File(PATH));
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            Log.v("PERMISSION","Permission is granted");
            downloadObserver =
                    transferUtility.download(
                            "iot-image",
                            "image.png",
                            new File(PATH));
        }

        // Attach a listener to the observer to get state update and progress notifications
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    Picasso.get().load(new File(PATH)).into(ivResult);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d("Your Activity", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
            }

        });

        // If you prefer to poll for the data, instead of attaching a
        // listener, check for the state and progress in the observer.
        if (TransferState.COMPLETED == downloadObserver.getState()) {
            Picasso.get().load(new File(PATH)).into(ivResult);
        }

        Log.d("Your Activity", "Bytes Transferred: " + downloadObserver.getBytesTransferred());
        Log.d("Your Activity", "Bytes Total: " + downloadObserver.getBytesTotal());
    }
}
