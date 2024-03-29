package com.tejus.shavedoggery.core;

public class Definitions {
    public static final long HEARTBEAT_INTERVAL = 1000;
    public static final String TYPE_HEARTBEAT = "type_heartbeat";
    public static final String PACKET_TYPE = "packet_type";
    public static final String STATUS = "status";
    public static final Object STATUS_ONLINE = "online";
    public static final String SERVER_IP = "23.21.239.205";
    public static final int SERVER_PORT = 64000;
    public static final int SERVER_UPLOAD_PORT = 65000;
    public static final String INTENT_INCOMING_FILE_REQUEST = "com.tejus.shavedoggery.incoming_file_request";
    public static final String INTENT_RECIPIENT_NOT_FOUND = "com.tejus.shavedoggery.recipient_not_found";
    public static final String INTENT_AVAILABLE_USERS = "com.tejus.shavedoggery.available_users";
    public static final int WRITE_BUFFER_SIZE = 1024 * 1024; // 1 MB read buffer
    public static String OUR_USERNAME;
    public static String credsPrefFile = "credsPrefFile";
    public static int MIN_USERNAME_LENGTH = 3;
    public static String prefUserName = "pUserName";
    public static String defaultUserName = "unregisteredUser";
    public static String fileToPush;

}
