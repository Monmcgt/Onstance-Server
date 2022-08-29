package me.monmcgt.code.onstance.server.socket;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.monmcgt.code.onstance.server.Var;
import me.monmcgt.code.onstance.server.helper.TimeCountHelper;
import me.monmcgt.code.onstance.server.storage.InstanceList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ClientHandler extends Thread {
    public static final long KEEP_ALIVE_DELAY = 5000;

    private final Socket socket;
    private final ExecutorService executorService;
    private final InstanceList instanceList;

    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    private String uid;

    private InstanceList.Instance instance;

    private List<Consumer<Object>> objectInputStreamAcceptorThreads;

    private TimeCountHelper timeCountHelper;
    private boolean isKeepAlive;
    private String keepAliveNumber;

    private boolean isInstanceExists;

    public ClientHandler(Socket socket, ExecutorService executorService, InstanceList instanceList) {
        this.socket = socket;
        this.executorService = executorService;
        this.instanceList = instanceList;

        try {
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.objectInputStreamAcceptorThreads = new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.init();
    }

    public void init() {
        try {
            /* {"type": "init", "uid": "123456789abcdef"} */
            String json = this.objectInputStream.readUTF();
            JsonObject jsonObjectInit = Var.JSON_PARSER.parse(json).getAsJsonObject();
            String type = jsonObjectInit.get("type").getAsString();
            if (!type.equals("init")) {
                throw new RuntimeException("Invalid type: " + type);
            }
            String uid = jsonObjectInit.get("uid").getAsString();
            int length = uid.length();
            if (this.instanceList.isAlreadyExists(uid)) {
                this.isInstanceExists = true;
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", "init");
                jsonObject.addProperty("success", false);
                jsonObject.addProperty("message", "Instance already exists");
                this.objectOutputStream.writeUTF(jsonObject.toString());
                this.objectOutputStream.flush();
                this.disconnect();
            } else if (length > 50) {
                // the instance is not actually exists but the length is too long
                this.isInstanceExists = true;
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", "init");
                jsonObject.addProperty("success", false);
                jsonObject.addProperty("message", "Instance uid is too long (max 50)");
                this.objectOutputStream.writeUTF(jsonObject.toString());
                this.objectOutputStream.flush();
                this.disconnect();
            } else {
                this.uid = uid;
                this.isInstanceExists = false;
                this.instance = InstanceList.Instance.builder().id(uid).build();
                this.instanceList.addInstance(this.instance);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("type", "init");
                jsonObject.addProperty("success", true);
                this.objectOutputStream.writeUTF(jsonObject.toString());
                this.objectOutputStream.flush();
                System.out.println("[Onstance] New client connected: " + this.socket.getInetAddress() + " (" + this.uid + ")");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean startRunning() {
        if (this.isInstanceExists) {
            return false;
        } else {
            super.start();
            return true;
        }
    }

    @Override
    public void run() {
        this.executorService.submit(() -> {
            while (this.socket.isConnected()) {
                try {
                    String string = this.objectInputStream.readUTF();
                    this.handleInputObject(string);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        this.executorService.submit(() -> {
            do {
                try {
                    this.timeCountHelper = TimeCountHelper.newInstance(10);
                    this.isKeepAlive = false;
                    this.keepAliveNumber = Utils.generateRandomKeepAliveNumber();
                    JsonObject keepAliveJsonObject = Utils.getKeepAliveJsonObject(this.keepAliveNumber);
                    this.objectOutputStream.writeUTF(keepAliveJsonObject.toString());
                    // true if flushing is successful
                    boolean b = Utils.flushTimeOut(this.objectOutputStream, 2500, this);
                    if (!b) {
                        // flush timeout
                        this.disconnect();
                        break;
                    }
                    while (this.timeCountHelper.isTimeLeft()) {
                        if (this.isKeepAlive) {
                            break;
                        }
                        Thread.sleep(500);
                    }
                    if (!this.isKeepAlive) {
                        this.disconnect();
                    } else {
                        Thread.sleep(KEEP_ALIVE_DELAY);
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            } while (this.isKeepAlive);
        });
    }

    public void handleInputObject(String string) {
        for (Consumer<Object> consumer : this.objectInputStreamAcceptorThreads) {
            consumer.accept(string);
        }
        JsonObject jsonObject = Var.JSON_PARSER.parse(string).getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        if (type.equals("keep-alive")) {
            String verifyCode = jsonObject.get("verify-code").getAsString();
            if (verifyCode.equals(this.keepAliveNumber)) {
                this.extendKeepAlive();
            }
        }
    }

    public void extendKeepAlive() {
        this.isKeepAlive = true;
    }

    public synchronized void disconnect() {
        try {
            this.socket.close();
            Thread.currentThread().interrupt();
            if (this.instanceList.isInstanceExists(this.instance)) {
                this.instanceList.removeInstance(this.instance);
//                System.out.println("Client disconnected: " + this.socket.getRemoteSocketAddress());
//                System.out.println("Instance size: " + this.instanceList.getInstances().size());
                System.out.println("[Onstance] Client disconnected: " + this.socket.getInetAddress() + " (" + this.uid + ")");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Utils {
        public static JsonObject getKeepAliveJsonObject(String verifyCode) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", "keep-alive");
            jsonObject.addProperty("verify-code", verifyCode);
            return jsonObject;
        }

        public static String generateRandomKeepAliveNumber() {
            int num = 0;
            while (num < 100000) {
                // 6 digits
                num = Var.RANDOM.nextInt(1000000);
            }
            return String.valueOf(num);
        }

        public static boolean flushTimeOut(ObjectOutputStream objectOutputStream, long millis, ClientHandler instance) {
            AtomicBoolean output = new AtomicBoolean(true);

            AtomicBoolean isFinished = new AtomicBoolean(false);

            Temp temp_1 = new Temp();
            Temp temp_2 = new Temp();

            temp_1.setObject(new Thread(() -> {
                try {
                    objectOutputStream.flush();
                    isFinished.set(true);
                } catch (IOException e) {
                    if (e instanceof SocketException) {
                        instance.disconnect();
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }));

            temp_2.setObject(new Thread(() -> {
                try {
                    Thread.sleep(millis);
                    if (!((Thread) temp_1.getObject()).isAlive() && !isFinished.get()) {
                        ((Thread) temp_1.getObject()).interrupt();
                        isFinished.set(false);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));

            ((Thread) temp_1.getObject()).start();
            ((Thread) temp_2.getObject()).start();

            while (!isFinished.get()) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return output.get();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Temp {
        public Object object;
    }
}
