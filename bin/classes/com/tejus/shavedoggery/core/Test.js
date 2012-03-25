var net = require('net');
var redis = require("redis");

client = redis.createClient();

client.on("error", function(err) {
	console.log("Error " + err);
});

var socketMap = new Array();

// uploader's username : downloader's socket mapping. populated by downloader.
var fileTransferMap = new Array();

net.createServer(
		function(socket) {

			socket.on('connect', function(data) {
				console.log("connection from : " + socket.remoteAddress
						+ ", port = " + socket.remotePort);
			});

			socket.on('data', function(data) {
				var obj = JSON.parse(data);
				var username = obj.username;
				var packetType = obj.packet_type;
				var fileName, fileSize;

				if (packetType == "bootup") {
					console.log("bootup from user = " + username);
					socketMap[username] = socket;
				}

				if (packetType == "file_push_req") {

					var payload = new Array();
					payload['packet_type'] = "file_push_req";
					payload['file_size'] = obj.file_size;
					payload['file_name'] = obj.file_name;
					payload['uploader_username'] = obj.username;

					console.log("gonna send push_req: " + JSON.stringify(payload));

					sendMessage(obj.to, JSON.stringify(payload));
				}

				if (packetType == "file_push_req_ack") {

					var payload = new Array();
					payload['packet_type'] = "file_push_req_ack";
					payload['downloader_username'] = obj.username;

					console.log("gonna send file_push_req_ack: "
							+ payload.toString());

					sendMessage(obj.to, payload);
				}

			});

			function sendMessage(recipient, message) {
				socketMap[recipient].write(message.toString());
			}

		}).listen(64000);

// ///File transfer server:

net.createServer(
		function(socket) {
			var iAmAnUploaderMyUsername = null;
			socket.on('connect', function(data) {
				console.log("connection from : " + socket.remoteAddress
						+ ", port = " + socket.remotePort);
			});

			socket.on('data', function(data) {
				var obj = JSON.parse(data);
				// only the first packet has packet_type. subsequent ones are
				// file
				// chunks (if this is an uploader).
				if (obj.packet_type != null) {
					// downloader must send uploader's username.
					if (obj.packet_type.equals('download_stream')) {
						console.log("Download stream opened from : "
								+ obj.username);
						fileTransferMap[obj.uploader_username] = socket;
					}

					if (obj.packet_type.equals('upload_stream')) {
						console.log("Upload stream opened from : "
								+ obj.username);
						iAmAnUploaderMyUsername = obj.username;
					}

				} else {
					if (iAmAnUploaderMyUsername != null) {
						console.log("Writing file chunk from = "
								+ iAmAnUploaderMyUsername);
						fileTransferMap[iAmAnUploaderMyUsername].write(data);
					}
				}

			});
		}).listen(65000);
