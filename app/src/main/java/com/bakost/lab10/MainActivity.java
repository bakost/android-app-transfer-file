package com.bakost.lab10;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothApp";
    private static final int PICK_FILE_REQUEST = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PERMISSION_LOCATION = 3;
    private static final int REQUEST_PERMISSION_STORAGE = 100;
    private static final int REQUEST_PERMISSION_BLUETOOTH_CONNECT = 101;
    private static final int REQUEST_PERMISSION_BLUETOOTH_SCAN = 102;
    private static final int CREATE_FILE_REQUEST_CODE = 200;
    // Стандартный UUID для SPP
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private Uri selectedFileUri = null;
    private TextView logTextView;
    private ScrollView logScrollView;

    // Добавляем поле для хранения сокета
    private BluetoothSocket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logTextView = findViewById(R.id.logTextView);
        logScrollView = findViewById(R.id.logScrollView);

        appendLog("onCreate: Запуск активности");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            appendLog("Bluetooth не поддерживается на этом устройстве");
            Toast.makeText(this, "Bluetooth не поддерживается на этом устройстве", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            appendLog("onCreate: Разрешение на Bluetooth Connect не выдано, вызываем запрос");
            requestBluetoothConnectPermission();
        } else {
            appendLog("onCreate: Разрешение на Bluetooth Connect уже выдано");
            if (!bluetoothAdapter.isEnabled()) {
                appendLog("onCreate: Bluetooth выключён, запрашиваем его включение");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            appendLog("onCreate: Разрешение на Bluetooth Scan не выдано, вызываем запрос");
            requestBluetoothScanPermission();
        } else {
            appendLog("onCreate: Разрешение на Bluetooth Scan уже выдано");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            appendLog("onCreate: Разрешение на геолокацию не выдано, запрашиваем");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
        } else {
            appendLog("onCreate: Разрешение на геолокацию уже выдано");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            appendLog("onCreate: Разрешение на чтение внешнего хранилища не выдано, вызываем запрос");
            requestStoragePermission();
        } else {
            appendLog("onCreate: Разрешение на чтение внешнего хранилища уже выдано");
        }

        // Настройка кнопок
        Button sendButton = findViewById(R.id.sendButton);
        Button receiveButton = findViewById(R.id.receiveButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appendLog("onClick: Нажата кнопка отправки файла");
                selectFile();
            }
        });

        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appendLog("onClick: Нажата кнопка приёма файла");
                startServer();
            }
        });

        logTextView = findViewById(R.id.logTextView);
        logScrollView = findViewById(R.id.logScrollView);

        appendLog("onCreate: Запуск активности");

        // Добавление логов для проверки статуса разрешений
        int permissionStatus = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT);
        appendLog("Статус разрешения BLUETOOTH_CONNECT: " + permissionStatus);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Логика для Android 13+ (API 33 и выше)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                appendLog("onCreate: Разрешение на Bluetooth Connect не выдано, вызываем запрос");
                requestBluetoothConnectPermission();
            } else {
                appendLog("onCreate: Разрешение на Bluetooth Connect уже выдано");
                if (!bluetoothAdapter.isEnabled()) {
                    appendLog("onCreate: Bluetooth выключён, запрашиваем его включение");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                appendLog("onCreate: Разрешение на Bluetooth Scan не выдано, вызываем запрос");
                requestBluetoothScanPermission();
            } else {
                appendLog("onCreate: Разрешение на Bluetooth Scan уже выдано");
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Логика для Android 12 (API 31)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                appendLog("onCreate: Разрешение на Bluetooth не выдано, вызываем запрос");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH},
                        REQUEST_PERMISSION_BLUETOOTH_CONNECT);
            } else {
                appendLog("onCreate: Разрешение на Bluetooth уже выдано");
                if (!bluetoothAdapter.isEnabled()) {
                    appendLog("onCreate: Bluetooth выключён, запрашиваем его включение");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        } else {
            // Логика для Android 11 и ниже (API 30 и ниже)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                appendLog("onCreate: Разрешение на Bluetooth не выдано, вызываем запрос");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH},
                        REQUEST_PERMISSION_BLUETOOTH_CONNECT);
            } else {
                appendLog("onCreate: Разрешение на Bluetooth уже выдано");
                if (!bluetoothAdapter.isEnabled()) {
                    appendLog("onCreate: Bluetooth выключён, запрашиваем его включение");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    }

    /**
     * Запрос разрешения на чтение внешнего хранилища
     */
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Log.d(TAG, "requestStoragePermission: Нужно показать rationale для чтения файлов");
            new AlertDialog.Builder(this)
                    .setTitle("Требуется разрешение")
                    .setMessage("Приложению необходимо разрешение на чтение файлов для выбора файла.")
                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "requestStoragePermission: Пользователь согласился, запрашиваем разрешение");
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_PERMISSION_STORAGE);
                        }
                    })
                    .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "requestStoragePermission: Пользователь отказался от разрешения");
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this,
                                    "Разрешение на чтение файлов необходимо для работы приложения.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .create()
                    .show();
        } else {
            Log.d(TAG, "requestStoragePermission: Rationale не требуется, запрашиваем разрешение напрямую");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_STORAGE);
        }
    }

    /**
     * Запрос разрешения на подключение к Bluetooth (BLUETOOTH_CONNECT)
     */
    private void requestBluetoothConnectPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.d(TAG, "requestBluetoothConnectPermission: Нужно показать rationale для Bluetooth Connect");
            new AlertDialog.Builder(this)
                    .setTitle("Требуется разрешение")
                    .setMessage("Приложению необходимо разрешение на подключение к Bluetooth устройствам для работы.")
                    .setPositiveButton("ОК", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                                REQUEST_PERMISSION_BLUETOOTH_CONNECT);
                    })
                    .setNegativeButton("Отмена", (dialog, which) -> {
                        Toast.makeText(this, "Разрешение необходимо для работы приложения", Toast.LENGTH_SHORT).show();
                    })
                    .create()
                    .show();
        } else {
            Log.d(TAG, "requestBluetoothConnectPermission: Rationale не требуется, запрашиваем разрешение напрямую");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_PERMISSION_BLUETOOTH_CONNECT);
        }
    }

    /**
     * Запрос разрешения на сканирование Bluetooth (BLUETOOTH_SCAN)
     */
    private void requestBluetoothScanPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.BLUETOOTH_SCAN)) {
            Log.d(TAG, "requestBluetoothScanPermission: Нужно показать rationale для Bluetooth Scan");
            new AlertDialog.Builder(this)
                    .setTitle("Требуется разрешение")
                    .setMessage("Приложению необходимо разрешение на сканирование Bluetooth устройств для работы.")
                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "requestBluetoothScanPermission: Пользователь согласился, запрашиваем разрешение");
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                                    REQUEST_PERMISSION_BLUETOOTH_SCAN);
                        }
                    })
                    .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "requestBluetoothScanPermission: Пользователь отказался от разрешения");
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this,
                                    "Разрешение на Bluetooth Scan необходимо для работы приложения.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .create()
                    .show();
        } else {
            Log.d(TAG, "requestBluetoothScanPermission: Rationale не требуется, запрашиваем разрешение напрямую");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_PERMISSION_BLUETOOTH_SCAN);
        }
    }

    /**
     * Запуск файлового менеджера для выбора файла.
     */
    private void selectFile() {
        Log.d(TAG, "selectFile: Запускаем Intent выбора файла");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Все типы файлов
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    /**
     * Метод для вызова SAF и создания нового файла.
     */
    private void createFileWithSAF(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                selectedFileUri = data.getData();
                Log.d(TAG, "onActivityResult: File URI: " + selectedFileUri);
                if (selectedFileUri != null) {
                    Toast.makeText(this, "Файл выбран: " + selectedFileUri, Toast.LENGTH_SHORT).show();
                    showPairedDevicesDialog();
                } else {
                    Toast.makeText(this, "Ошибка: выбранный файл недоступен.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Файл не выбран.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Bluetooth должен быть включен!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == CREATE_FILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    // Получаем поток для записи в созданный файл
                    try (InputStream inputStream = socket != null ? socket.getInputStream() : null;
                         OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        if (inputStream != null && outputStream != null) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            outputStream.flush();
                            Toast.makeText(this, "Файл успешно создан: " + uri.getPath(), Toast.LENGTH_SHORT).show();
                            appendLog("Файл успешно создан: " + uri.getPath());
                        } else {
                            Toast.makeText(this, "Ошибка при создании файла", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        appendLog("Ошибка при создании файла: " + e.getMessage());
                    }
                }
            } else if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
                if (data != null && data.getData() != null) {
                    Uri fileUri = data.getData();
                    appendLog("SAF: Файл создан: " + fileUri.toString());

                    // Сохранение данных в выбранный файл
                    new Thread(() -> {
                        try (OutputStream os = getContentResolver().openOutputStream(fileUri)) {
                            if (os != null) {
                                os.write("Пример данных".getBytes()); // Здесь можно записывать реальные данные
                                appendLog("SAF: Данные успешно записаны в файл");
                            }
                        } catch (IOException e) {
                            appendLog("SAF: Ошибка записи в файл: " + e.getMessage());
                        }
                    }).start();
                } else {
                    appendLog("SAF: Не удалось получить URI файла");
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("Разрешение на Bluetooth Connect получено");
            } else {
                appendLog("Разрешение на Bluetooth Connect отклонено");
                Toast.makeText(this, "Разрешение на Bluetooth Connect необходимо для работы приложения", Toast.LENGTH_SHORT).show();
                // Предложить пользователю открыть настройки приложения
                new AlertDialog.Builder(this)
                        .setTitle("Требуется разрешение")
                        .setMessage("Приложению необходимо разрешение на подключение к Bluetooth устройствам. Откройте настройки приложения и включите разрешение.")
                        .setPositiveButton("Открыть настройки", (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Отмена", null)
                        .create()
                        .show();
            }
        }
    }

    /**
     * Отображение диалога со списком сопряжённых Bluetooth устройств.
     */
    private void showPairedDevicesDialog() {
        // Перед получением списка устройств проверяем BLUETOOTH_CONNECT
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "showPairedDevicesDialog: Разрешение BLUETOOTH_CONNECT не получено");
            Toast.makeText(this, "Разрешение на Bluetooth Connect не получено", Toast.LENGTH_SHORT).show();
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Log.d(TAG, "showPairedDevicesDialog: Нет сопряжённых устройств");
            Toast.makeText(this, "Нет сопряжённых Bluetooth устройств", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "showPairedDevicesDialog: Найдено сопряжённых устройств: " + pairedDevices.size());
        final BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
        String[] deviceNames = new String[devices.length];
        for (int i = 0; i < devices.length; i++) {
            deviceNames[i] = devices[i].getName() + "\n" + devices[i].getAddress();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите устройство для отправки файла");
        builder.setItems(deviceNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothDevice device = devices[which];
                Log.d(TAG, "showPairedDevicesDialog: Выбрано устройство " + device.getName());
                new Thread(new ClientThread(device, selectedFileUri)).start();
            }
        });
        builder.show();
    }

    /**
     * Запуск серверного потока для приема файла по Bluetooth.
     */
    private void startServer() {
        new Thread(new ServerThread()).start();
        Toast.makeText(this, "Сервер запущен. Ожидание входящего файла...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "startServer: Сервер запущен");
    }

    /**
     * Клиентский поток для отправки файла выбранному устройству.
     */
    private class ClientThread implements Runnable {
        private BluetoothDevice device;
        private Uri fileUri;

        ClientThread(BluetoothDevice device, Uri fileUri) {
            this.device = device;
            this.fileUri = fileUri;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            try {
                appendLog("ClientThread: Подключение к устройству " + device.getName());
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothAdapter.cancelDiscovery();
                socket.connect();
                appendLog("ClientThread: Соединение установлено с " + device.getName());

                OutputStream os = socket.getOutputStream();
                InputStream is = getContentResolver().openInputStream(fileUri);
                if (is == null) {
                    appendLog("ClientThread: Не удалось открыть поток для выбранного файла");
                    return;
                }

                // Отправка имени файла и содержимого в одном потоке
                String fileName = new File(fileUri.getPath()).getName();
                byte[] fileNameBytes = fileName.getBytes();
                byte[] fileNameLength = new byte[4];
                fileNameLength[0] = (byte) (fileNameBytes.length >> 24);
                fileNameLength[1] = (byte) (fileNameBytes.length >> 16);
                fileNameLength[2] = (byte) (fileNameBytes.length >> 8);
                fileNameLength[3] = (byte) (fileNameBytes.length);
                os.write(fileNameLength);
                os.write(fileNameBytes);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                appendLog("ClientThread: Файл успешно отправлен");
                is.close();
                os.close();
            } catch (IOException e) {
                appendLog("ClientThread: Ошибка при отправке файла: " + e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        appendLog("ClientThread: Ошибка при закрытии сокета");
                    }
                }
            }
        }
    }

    /**
     * Серверный поток для приема файла по Bluetooth.
     */
    private class ServerThread implements Runnable {
        @Override
        public void run() {
            BluetoothServerSocket serverSocket = null;
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothApp", MY_UUID);
                appendLog("ServerThread: Сервер запущен. Ожидание соединения...");
                socket = serverSocket.accept();
                appendLog("ServerThread: Соединение принято от: " + socket.getRemoteDevice().getName());

                if (!socket.isConnected()) {
                    appendLog("ServerThread: Сокет не подключен, завершение работы");
                    return;
                }

                InputStream is = socket.getInputStream();

                // Чтение имени файла
                byte[] fileNameLength = new byte[4];
                if (is.read(fileNameLength) != 4) {
                    appendLog("ServerThread: Ошибка чтения длины имени файла");
                    return;
                }
                int length = ((fileNameLength[0] & 0xFF) << 24) | ((fileNameLength[1] & 0xFF) << 16) | ((fileNameLength[2] & 0xFF) << 8) | (fileNameLength[3] & 0xFF);
                byte[] fileNameBytes = new byte[length];
                if (is.read(fileNameBytes) != length) {
                    appendLog("ServerThread: Ошибка чтения имени файла");
                    return;
                }
                String receivedFileName = new String(fileNameBytes);

                // Вызов SAF для сохранения файла с полученным именем
                createFileWithSAF(receivedFileName);

                // Чтение содержимого файла и сохранение
                Uri fileUri = getFileUri(); // Получение URI, предоставленного SAF
                if (fileUri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(fileUri)) {
                        if (os != null) {
                            byte[] buffer = new byte[8192]; // Увеличение размера буфера для повышения производительности
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                            os.flush();
                        } else {
                            appendLog("ServerThread: Ошибка открытия потока для записи");
                        }
                    } catch (IOException e) {
                        appendLog("ServerThread: Ошибка записи в файл: " + e.getMessage());
                    }
                } else {
                    appendLog("ServerThread: URI для сохранения файла не найден");
                }
            } catch (IOException e) {
                appendLog("ServerThread: Ошибка при приёме соединения: " + e.getMessage());
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                        appendLog("ServerThread: Серверный сокет закрыт");
                    } catch (IOException e) {
                        appendLog("ServerThread: Ошибка при закрытии серверного сокета: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            logTextView.append(message + "\n");
            logScrollView.post(() -> logScrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    // Метод для получения URI, предоставленного SAF
    private Uri getFileUri() {
        if (selectedFileUri != null) {
            return selectedFileUri;
        } else {
            appendLog("getFileUri: URI не найден. Убедитесь, что файл был выбран через SAF.");
            return null;
        }
    }
}
