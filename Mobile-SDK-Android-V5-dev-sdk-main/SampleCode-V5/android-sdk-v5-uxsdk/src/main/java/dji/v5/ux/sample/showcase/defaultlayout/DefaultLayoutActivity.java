/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.sample.showcase.defaultlayout;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.video.channel.VideoChannelState;
import dji.v5.common.video.channel.VideoChannelType;
import dji.v5.common.video.interfaces.IVideoChannel;
import dji.v5.common.video.interfaces.VideoChannelStateChangeListener;
import dji.v5.common.video.stream.PhysicalDevicePosition;
import dji.v5.common.video.stream.StreamSource;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.network.DJINetworkManager;
import dji.v5.network.IDJINetworkStatusListener;
import dji.v5.utils.common.JsonUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.accessory.RTKStartServiceHelper;
import dji.v5.ux.cameracore.widget.autoexposurelock.AutoExposureLockWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.CameraControlsWidget;
import dji.v5.ux.cameracore.widget.cameracontrols.exposuresettings.ExposureSettingsPanel;
import dji.v5.ux.cameracore.widget.cameracontrols.lenscontrol.LensControlWidget;
import dji.v5.ux.cameracore.widget.focusexposureswitch.FocusExposureSwitchWidget;
import dji.v5.ux.cameracore.widget.focusmode.FocusModeWidget;
import dji.v5.ux.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.communication.BroadcastValues;
import dji.v5.ux.core.communication.GlobalPreferenceKeys;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.communication.UXKeys;
import dji.v5.ux.core.extension.ViewExtensions;
import dji.v5.ux.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.v5.ux.core.panel.topbar.TopBarPanelWidget;
import dji.v5.ux.core.util.CameraUtil;
import dji.v5.ux.core.util.CommonUtils;
import dji.v5.ux.core.util.DataProcessor;
import dji.v5.ux.core.util.ViewUtil;
import dji.v5.ux.core.widget.fpv.FPVWidget;
import dji.v5.ux.core.widget.hsi.HorizontalSituationIndicatorWidget;
import dji.v5.ux.core.widget.hsi.PrimaryFlightDisplayWidget;
import dji.v5.ux.core.widget.setting.SettingWidget;
import dji.v5.ux.core.widget.simulator.SimulatorIndicatorWidget;
import dji.v5.ux.core.widget.systemstatus.SystemStatusWidget;
import dji.v5.ux.gimbal.GimbalFineTuneWidget;
import dji.v5.ux.training.simulatorcontrol.SimulatorControlWidget;
import dji.v5.ux.visualcamera.CameraNDVIPanelWidget;
import dji.v5.ux.visualcamera.CameraVisiblePanelWidget;
import dji.v5.ux.visualcamera.zoom.FocalZoomWidget;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import android.graphics.Bitmap;
import android.widget.Button;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.HashMap;
import java.util.Map;


/**
 * Displays a sample layout of widgets similar to that of the various DJI apps.
 */
public class DefaultLayoutActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FPVWidget fpvWidget;
    private Button btnScanAndSend;
    private Handler handler;
    private Runnable runnable;
    private int delayMillis = 4000;
    private int delayMillis2 = 2000;
    private boolean isScanning = false; // Variável para rastrear se o scanner está em execução


    //region Fields
    private final String TAG = LogUtils.getTag(this);

    protected FPVWidget primaryFpvWidget;
    protected FPVInteractionWidget fpvInteractionWidget;
    protected FPVWidget secondaryFPVWidget;
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;
    protected SimulatorControlWidget simulatorControlWidget;
    protected LensControlWidget lensControlWidget;
    protected AutoExposureLockWidget autoExposureLockWidget;
    protected FocusModeWidget focusModeWidget;
    protected FocusExposureSwitchWidget focusExposureSwitchWidget;
    protected CameraControlsWidget cameraControlsWidget;
    protected HorizontalSituationIndicatorWidget horizontalSituationIndicatorWidget;
    protected ExposureSettingsPanel exposureSettingsPanel;
    protected PrimaryFlightDisplayWidget pfvFlightDisplayWidget;
    protected CameraNDVIPanelWidget ndviCameraPanel;
    protected CameraVisiblePanelWidget visualCameraPanel;
    protected FocalZoomWidget focalZoomWidget;
    protected SettingWidget settingWidget;
    protected TopBarPanelWidget topBarPanel;
    protected ConstraintLayout fpvParentView;
    private DrawerLayout mDrawerLayout;
    private TextView gimbalAdjustDone;
    private GimbalFineTuneWidget gimbalFineTuneWidget;


    private CompositeDisposable compositeDisposable;
    private final DataProcessor<CameraSource> cameraSourceProcessor = DataProcessor.create(new CameraSource(PhysicalDevicePosition.UNKNOWN,
            CameraLensType.UNKNOWN));
    private VideoChannelStateChangeListener primaryChannelStateListener = null;
    private VideoChannelStateChangeListener secondaryChannelStateListener = null;
    private final IDJINetworkStatusListener networkStatusListener = isNetworkAvailable -> {
        if (isNetworkAvailable) {
            LogUtils.d(TAG, "isNetworkAvailable=" + true);
            RTKStartServiceHelper.INSTANCE.startRtkService(false);
        }
    };

    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_default_layout);
        fpvParentView = findViewById(R.id.fpv_holder);
        mDrawerLayout = findViewById(R.id.root_view);
        topBarPanel = findViewById(R.id.panel_top_bar);
        settingWidget = topBarPanel.getSettingWidget();
        primaryFpvWidget = findViewById(R.id.widget_primary_fpv);
        fpvInteractionWidget = findViewById(R.id.widget_fpv_interaction);
        secondaryFPVWidget = findViewById(R.id.widget_secondary_fpv);
        systemStatusListPanelWidget = findViewById(R.id.widget_panel_system_status_list);
        simulatorControlWidget = findViewById(R.id.widget_simulator_control);
        lensControlWidget = findViewById(R.id.widget_lens_control);
        ndviCameraPanel = findViewById(R.id.panel_ndvi_camera);
        visualCameraPanel = findViewById(R.id.panel_visual_camera);
        autoExposureLockWidget = findViewById(R.id.widget_auto_exposure_lock);
        focusModeWidget = findViewById(R.id.widget_focus_mode);
        focusExposureSwitchWidget = findViewById(R.id.widget_focus_exposure_switch);
        exposureSettingsPanel = findViewById(R.id.panel_camera_controls_exposure_settings);
        pfvFlightDisplayWidget = findViewById(R.id.widget_fpv_flight_display_widget);
        focalZoomWidget = findViewById(R.id.widget_focal_zoom);
        cameraControlsWidget = findViewById(R.id.widget_camera_controls);
        horizontalSituationIndicatorWidget = findViewById(R.id.widget_horizontal_situation_indicator);
        gimbalAdjustDone = findViewById(R.id.fpv_gimbal_ok_btn);
        gimbalFineTuneWidget = findViewById(R.id.setting_menu_gimbal_fine_tune);

        cameraControlsWidget.getExposureSettingsIndicatorWidget().setStateChangeResourceId(R.id.panel_camera_controls_exposure_settings);

        initClickListener();
        MediaDataCenter.getInstance().getVideoStreamManager().addStreamSourcesListener(sources -> runOnUiThread(() -> updateFPVWidgetSource(sources)));
        primaryFpvWidget.setOnFPVStreamSourceListener((devicePosition, lensType) -> {
            cameraSourceProcessor.onNext(new CameraSource(devicePosition, lensType));
        });

        //小surfaceView放置在顶部，避免被大的遮挡
        secondaryFPVWidget.setSurfaceViewZOrderOnTop(true);
        secondaryFPVWidget.setSurfaceViewZOrderMediaOverlay(true);


        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        //实现RTK监测网络，并自动重连机制
        DJINetworkManager.getInstance().addNetworkStatusListener(networkStatusListener);


        // Inicializa o FPVWidget
        fpvWidget = findViewById(R.id.widget_primary_fpv);

        // Inicialize o Firestore
        db = FirebaseFirestore.getInstance();

        // Inicialize o botão
        btnScanAndSend = findViewById(R.id.btnStartAndStop);

        // Inicialize o handler
        handler = new Handler();

        // Inicialize o runnable
        runnable = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    if (paletCode == null || posicaoCode== null ) {
                        scanBarcodeAndSendToFirestore();
                        handler.postDelayed(this, delayMillis);

                    } else {
                        sendDataToSqlServer(() -> handler.postDelayed(this, delayMillis));

                    }
                }
            }
        };

        btnScanAndSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Alternar entre iniciar e parar o scanner
                if (isScanning) {
                    stopScanning();
                } else {
                    startScanning();
                }
            }
        });

        Button btnEmpty = findViewById(R.id.btn_empty);

        // Adicionar um ouvinte de clique ao botão
        btnEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmEmpty();
            }
        });


    }

    private void startScanning() {
        isScanning = true;
        btnScanAndSend.setText("Stop"); // Atualizar texto do botão para "Stop"
        // Inicie o scanner quando o botão for pressionado
        scanBarcodeAndSendToFirestore();
        // Inicie chamadas recorrentes
        handler.postDelayed(runnable, delayMillis);
        updateTextView("palet", paletCode);
        updateTextView("posicao", posicaoCode);
    }

    private void stopScanning() {
        paletCode = null;
        posicaoCode = null;
        isScanning = false;
        btnScanAndSend.setText("Start"); // Atualizar texto do botão para "Start"
        // Remover chamadas recorrentes
        handler.removeCallbacks(runnable);
        updateTextView("palet", paletCode);
        updateTextView("posicao", posicaoCode);

    }

    private void showConfirmEmpty() {
        // Crie um AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmação");
        builder.setMessage("Deseja confirmar que o palet está vazio?");

        // Adicione os botões "Confirmar" e "Cancelar"
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Ao clicar em "Confirmar", atribuir "0" à variável paletCode
                paletCode = "0";
                showToast("Valor '0' atribuído a 'paletCode'.");
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Ao clicar em "Cancelar", não fazer nada
                showToast("Ação cancelada.");
            }
        });

        // Exiba o AlertDialog
        builder.show();
    }


    private String paletCode; // Variável para armazenar o código quando começa com 750 ou 200
    private String posicaoCode; // Variável para armazenar o código em outros casos
    private Bitmap croppedBitmap;


    @RequiresApi(api = Build.VERSION_CODES.N)
    public void scanBarcodeAndSendToFirestore() {
        Bitmap capturedBitmap = fpvWidget.captureBitmap();


        // Se o bitmap não for nulo, prossiga com o processamento do código de barras
        if (capturedBitmap  != null) {
            // Calcular a ROI (Region of Interest)
            int width = capturedBitmap .getWidth();
            int height = capturedBitmap .getHeight();
            int rectWidth = width / 2;
            int rectHeight = height / 2;
            int x = (width - rectWidth) / 2;
            int y = (height - rectHeight) / 2;

            // Corrija a inicialização da variável de instância em vez de criar uma local
            croppedBitmap = Bitmap.createBitmap(capturedBitmap , x, y, rectWidth, rectHeight);

            // Usar a imagem recortada para a digitalização do código de barras
            InputImage image = InputImage.fromBitmap(croppedBitmap, 0);

            BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_CODE_128)
                    .build();

            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty()) {
                            String barcodeValue = barcodes.get(0).getRawValue();

                            // Verificar se o código de barras começa com "750" ou "200"
                            if (barcodeValue.startsWith("750") || barcodeValue.startsWith("200")){
                                if (paletCode == null) {
                                    paletCode = barcodeValue;
                                    updateTextView("palet", paletCode);

                                } else if (posicaoCode == null) {
                                    showToast("Leia o código da Posição");}

                            } else if (barcodeValue.matches("\\d{2}-\\d{2}-\\d{3}")){
                                if (posicaoCode == null) {
                                    posicaoCode = barcodeValue;
                                    updateTextView("posicao", posicaoCode);
                                } else if (paletCode == null) {
                                    showToast("Leia o código do Palet");}

                            } else {
                                showToast("Código Inválido");}

                        } else {
                            showToast("Nenhum código de barras encontrado");
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Tratar erro
                    });
        } else {
            showToast("Falha ao obter o bitmap do FPVWidget");
        }
    }

    private void updateTextView(String textViewName, String value) {
        switch (textViewName) {
            case "palet":
                // Atualizar a TextView "palet" com o valor do código
                // Certifique-se de substituir R.id.textview_palet pelo ID real da sua TextView "palet"
                TextView paletTextView = findViewById(R.id.palet);
                if (paletTextView != null) {
                    paletTextView.setText(value);
                }
                break;
            case "posicao":
                // Atualizar a TextView "posicao" com o valor do código
                // Certifique-se de substituir R.id.textview_posicao pelo ID real da sua TextView "posicao"
                TextView posicaoTextView = findViewById(R.id.posicao);
                if (posicaoTextView != null) {
                    posicaoTextView.setText(value);
                }
                break;
            default:
                break;
        }
    }


    private void sendDataToSqlServer(Runnable onCompletion) {
        // Obtenha a data atual no formato "dd-MM-yyyy"
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        // Crie uma string de conexão JDBC para o SQL Server
        String DB_URL = "jdbc:jtds:sqlserver://10.0.0.148:1433/AdventureWorksDW2019";
        String DB_USER = "sa";
        String DB_PASSWORD = "Kristyson";

        // Crie um objeto Connection para se conectar ao banco de dados
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Crie um objeto PreparedStatement para inserir os dados na tabela
            String query = "INSERT INTO dbo.AdventureWorksDWBuildVersion (palet, data) VALUES (?, ?)";
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, paletCode);
            statement.setString(2, currentDate);

            // Execute a inserção de dados
            int rowsInserted = statement.executeUpdate();

            // Verifique se a inserção foi bem-sucedida
            if (rowsInserted > 0) {
                // Ação bem-sucedida, por exemplo, exibir um Toast
                // ou realizar outras operações após o sucesso
                showToast("Dados enviados para o SQL Server: '" + currentDate + "'!");
                if (onCompletion != null) {
                    onCompletion.run();
                }
            } else {
                // Tratamento de falha em caso de falha na inserção
                showToast("Erro ao enviar dados para o SQL Server.");
            }
        } catch (SQLException e) {
            // Tratamento de exceção em caso de erro de conexão ou consulta SQL
            showToast("Erro ao conectar ao SQL Server: " + e.getMessage());
        }

        // Limpe as variáveis após o envio
        paletCode = null;
        posicaoCode = null;
        updateTextView("palet", "");
        updateTextView("posicao", "");
    }




    /*private void sendDataToFirestore(Runnable onCompletion) {
        // Obtenha a data atual no formato "dd-MM-yyyy"
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());

        // Crie um mapa para representar os dados que você deseja enviar
        Map<String, Object> data = new HashMap<>();
        data.put("palet", paletCode);

        db.collection(currentDate) // Use a data como nome da coleção
                .document(posicaoCode) // Use a variável posicaoCode como nome do documento
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    // Ação bem-sucedida, por exemplo, exibir um Toast
                    // ou realizar outras operações após o sucesso
                    showToast("Dados enviados > '" + currentDate + "'!");
                    if (onCompletion != null) {
                        onCompletion.run();
                    }
                })
                .addOnFailureListener(e -> {
                    // Tratamento de falha, por exemplo, exibir um Toast
                    // ou realizar outras operações em caso de falha
                    showToast("Erro ao enviar dados: " + e.getMessage());
                });
        // Limpe as variáveis após o envio
        paletCode = null;
        posicaoCode = null;
        updateTextView("palet", "");
        updateTextView("posicao", "");
    }*/

    private void showToast(String message) {
        // Método auxiliar para exibir um Toast
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    

    private void isGimableAdjustClicked(BroadcastValues broadcastValues) {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawers();
        }
        horizontalSituationIndicatorWidget.setVisibility(View.GONE);
        if (gimbalFineTuneWidget != null) {
            gimbalFineTuneWidget.setVisibility(View.VISIBLE);
        }
    }

    private void initClickListener() {
        secondaryFPVWidget.setOnClickListener(v -> swapVideoSource());
        initChannelStateListener();

        if (settingWidget != null) {
            settingWidget.setOnClickListener(v -> toggleRightDrawer());
        }

        // Setup top bar state callbacks
        SystemStatusWidget systemStatusWidget = topBarPanel.getSystemStatusWidget();
        if (systemStatusWidget != null) {
            systemStatusWidget.setOnClickListener(v -> ViewExtensions.toggleVisibility(systemStatusListPanelWidget));
        }

        SimulatorIndicatorWidget simulatorIndicatorWidget = topBarPanel.getSimulatorIndicatorWidget();
        if (simulatorIndicatorWidget != null) {
            simulatorIndicatorWidget.setOnClickListener(v -> ViewExtensions.toggleVisibility(simulatorControlWidget));
        }
        gimbalAdjustDone.setOnClickListener(view -> {
            horizontalSituationIndicatorWidget.setVisibility(View.VISIBLE);
            if (gimbalFineTuneWidget != null) {
                gimbalFineTuneWidget.setVisibility(View.GONE);
            }

        });
    }

    private void toggleRightDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.END);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        MediaDataCenter.getInstance().getVideoStreamManager().clearAllStreamSourcesListeners();
        removeChannelStateListener();
        DJINetworkManager.getInstance().removeNetworkStatusListener(networkStatusListener);

    }

    @Override
    protected void onResume() {
        super.onResume();

        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pressed -> {
                    if (pressed) {
                        ViewExtensions.hide(systemStatusListPanelWidget);
                    }
                }));
        compositeDisposable.add(simulatorControlWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(simulatorControlWidgetState -> {
                    if (simulatorControlWidgetState instanceof SimulatorControlWidget.UIState.VisibilityUpdated) {
                        if (((SimulatorControlWidget.UIState.VisibilityUpdated) simulatorControlWidgetState).isVisible()) {
                            hideOtherPanels(simulatorControlWidget);
                        }
                    }
                }));
        compositeDisposable.add(cameraSourceProcessor.toFlowable()
                .observeOn(SchedulerProvider.io())
                .throttleLast(500, TimeUnit.MILLISECONDS)
                .subscribeOn(SchedulerProvider.io())
                .subscribe(result -> runOnUiThread(() -> onCameraSourceUpdated(result.devicePosition, result.lensType)))
        );
        compositeDisposable.add(ObservableInMemoryKeyedStore.getInstance()
                .addObserver(UXKeys.create(GlobalPreferenceKeys.GIMBAL_ADJUST_CLICKED))
                .observeOn(SchedulerProvider.ui())
                .subscribe(this::isGimableAdjustClicked));
        ViewUtil.setKeepScreen(this, true);
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }

        super.onPause();
        ViewUtil.setKeepScreen(this, false);
    }
    //endregion

    private void hideOtherPanels(@Nullable View widget) {
        View[] panels = {
                simulatorControlWidget
        };

        for (View panel : panels) {
            if (widget != panel) {
                panel.setVisibility(View.GONE);
            }
        }
    }

    private void updateFPVWidgetSource(List<StreamSource> streamSources) {
        LogUtils.i(TAG, JsonUtil.toJson(streamSources));
        if (streamSources == null) {
            return;
        }

        //没有数据
        if (streamSources.isEmpty()) {
            secondaryFPVWidget.setVisibility(View.GONE);
            return;
        }

        //仅一路数据
        if (streamSources.size() == 1) {
            //这里仅仅做Widget的显示与否，source和channel的获取放到widget中
            secondaryFPVWidget.setVisibility(View.GONE);
            return;
        }
        secondaryFPVWidget.setVisibility(View.VISIBLE);
    }

    private void initChannelStateListener() {
        IVideoChannel primaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL);
        IVideoChannel secondaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL);
        if (primaryChannel != null) {
            primaryChannelStateListener = (from, to) -> {
                StreamSource primaryStreamSource = primaryChannel.getStreamSource();
                if (VideoChannelState.ON == to && primaryStreamSource != null) {
                    runOnUiThread(() -> primaryFpvWidget.updateVideoSource(primaryStreamSource, VideoChannelType.PRIMARY_STREAM_CHANNEL));
                }
            };
            primaryChannel.addVideoChannelStateChangeListener(primaryChannelStateListener);
        }
        if (secondaryChannel != null) {
            secondaryChannelStateListener = (from, to) -> {
                StreamSource secondaryStreamSource = secondaryChannel.getStreamSource();
                if (VideoChannelState.ON == to && secondaryStreamSource != null) {
                    runOnUiThread(() -> secondaryFPVWidget.updateVideoSource(secondaryStreamSource, VideoChannelType.SECONDARY_STREAM_CHANNEL));
                }
            };
            secondaryChannel.addVideoChannelStateChangeListener(secondaryChannelStateListener);
        }
    }

    private void removeChannelStateListener() {
        IVideoChannel primaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.PRIMARY_STREAM_CHANNEL);
        IVideoChannel secondaryChannel =
                MediaDataCenter.getInstance().getVideoStreamManager().getAvailableVideoChannel(VideoChannelType.SECONDARY_STREAM_CHANNEL);
        if (primaryChannel != null) {
            primaryChannel.removeVideoChannelStateChangeListener(primaryChannelStateListener);
        }
        if (secondaryChannel != null) {
            secondaryChannel.removeVideoChannelStateChangeListener(secondaryChannelStateListener);
        }
    }

    private void onCameraSourceUpdated(PhysicalDevicePosition devicePosition, CameraLensType lensType) {
        LogUtils.i(TAG, devicePosition, lensType);
        ComponentIndexType cameraIndex = CameraUtil.getCameraIndex(devicePosition);
        updateViewVisibility(devicePosition, lensType);
        updateInteractionEnabled();
        //如果无需使能或者显示的，也就没有必要切换了。
        if (fpvInteractionWidget.isInteractionEnabled()) {
            fpvInteractionWidget.updateCameraSource(cameraIndex, lensType);
            fpvInteractionWidget.updateGimbalIndex(CommonUtils.getGimbalIndex(devicePosition));
        }
        if (lensControlWidget.getVisibility() == View.VISIBLE) {
            lensControlWidget.updateCameraSource(cameraIndex, lensType);
        }
        if (ndviCameraPanel.getVisibility() == View.VISIBLE) {
            ndviCameraPanel.updateCameraSource(cameraIndex, lensType);
        }
        if (visualCameraPanel.getVisibility() == View.VISIBLE) {
            visualCameraPanel.updateCameraSource(cameraIndex, lensType);
        }
        if (autoExposureLockWidget.getVisibility() == View.VISIBLE) {
            autoExposureLockWidget.updateCameraSource(cameraIndex, lensType);
        }
        if (focusModeWidget.getVisibility() == View.VISIBLE) {
            focusModeWidget.updateCameraSource(cameraIndex, lensType);
        }
        if (focusExposureSwitchWidget.getVisibility() == View.VISIBLE) {
            focusExposureSwitchWidget.updateCameraSource(cameraIndex, lensType);
        }
        if (cameraControlsWidget.getVisibility() == View.VISIBLE) {
            cameraControlsWidget.updateCameraSource(cameraIndex, lensType);
        }
        if (exposureSettingsPanel.getVisibility() == View.VISIBLE) {
            exposureSettingsPanel.updateCameraSource(cameraIndex, lensType);
        }
        if (focalZoomWidget.getVisibility() == View.VISIBLE) {
            focalZoomWidget.updateCameraSource(cameraIndex, lensType);
        }
        if (horizontalSituationIndicatorWidget.getVisibility() == View.VISIBLE) {
            horizontalSituationIndicatorWidget.updateCameraSource(cameraIndex, lensType);
        }
    }

    private void updateViewVisibility(PhysicalDevicePosition devicePosition, CameraLensType lensType) {
        //只在fpv下显示
        pfvFlightDisplayWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.VISIBLE : View.INVISIBLE);

        //fpv下不显示
        lensControlWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        ndviCameraPanel.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        visualCameraPanel.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        autoExposureLockWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        focusModeWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        focusExposureSwitchWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        cameraControlsWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        focalZoomWidget.setVisibility(devicePosition == PhysicalDevicePosition.NOSE ? View.INVISIBLE : View.VISIBLE);
        horizontalSituationIndicatorWidget.setSimpleModeEnable(devicePosition != PhysicalDevicePosition.NOSE);

        //有其他的显示逻辑，这里确保fpv下不显示
        if (devicePosition == PhysicalDevicePosition.NOSE) {
            exposureSettingsPanel.setVisibility(View.INVISIBLE);
        }

        //只在部分len下显示
        ndviCameraPanel.setVisibility(CameraUtil.isSupportForNDVI(lensType) ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private void swapVideoSource() {
        VideoChannelType primaryVideoChannel = primaryFpvWidget.getVideoChannelType();
        StreamSource primaryStreamSource = primaryFpvWidget.getStreamSource();
        VideoChannelType secondaryVideoChannel = secondaryFPVWidget.getVideoChannelType();
        StreamSource secondaryStreamSource = secondaryFPVWidget.getStreamSource();
        //两个source都存在的情况下才进行切换
        if (secondaryStreamSource != null && primaryStreamSource != null) {
            primaryFpvWidget.updateVideoSource(secondaryStreamSource, secondaryVideoChannel);
            secondaryFPVWidget.updateVideoSource(primaryStreamSource, primaryVideoChannel);
        }
    }

    private void updateInteractionEnabled() {
        StreamSource newPrimaryStreamSource = primaryFpvWidget.getStreamSource();
        fpvInteractionWidget.setInteractionEnabled(false);
        if (newPrimaryStreamSource != null) {
            fpvInteractionWidget.setInteractionEnabled(newPrimaryStreamSource.getPhysicalDevicePosition() != PhysicalDevicePosition.NOSE);
        }
    }

    private static class CameraSource {
        PhysicalDevicePosition devicePosition;
        CameraLensType lensType;

        public CameraSource(PhysicalDevicePosition devicePosition, CameraLensType lensType) {
            this.devicePosition = devicePosition;
            this.lensType = lensType;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }
}
