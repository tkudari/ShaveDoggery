var net = require('net');
var redis = require("redis");

client = redis.createClient();

client.on("error", function(err) {
	console.log("Error " + err);
});

var socketMap = new Array();

// uploader's username : downloader's socket mapping. populated by downloader.
var fileTransferMap = new Array();
var usersStatus = new Array();
var listOfUsers = new Array();
var testReceiverSocket;

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
					listOfUsers.push(username);
				}

				if (packetType == "file_push_req") {

					var payload = {};
					payload.packet_type = "file_push_req";
					payload.file_size = obj.file_size;
					payload.file_name = obj.file_name;
					payload.uploader_username = obj.username;
					console.log("filename = " + obj.file_name);
					console.log("gonna send push_req: "
							+ JSON.stringify(payload));

					try {
						sendMessage(obj.to, JSON.stringify(payload));
					} catch (err) {
						// for now, we assume this happened cos the recipient doesn't exist yet:
						var payload = {};
						payload.packet_type = "recipient_not_found";
						payload.unknown_recipient = obj.to;
						console.log("recipient not found, replying back to: "
								+ obj.username);
						sendMessage(obj.username, JSON.stringify(payload));
					}
				}

				if (packetType == "file_push_req_ack") {

					var payload = {};
					payload.packet_type = "file_push_req_ack";
					payload.downloader_username = obj.username;

					console.log("gonna send file_push_req_ack: "
							+ JSON.stringify(payload));

					sendMessage(obj.to, JSON.stringify(payload));
				}
				
				//status request recvd.:
				if (packetType == "receivers_status_req") {
					queryEveryone();
					
					//reply after 2 seconds:
					setTimeout(function(){ 
						console.log("replying with users' statuses to: " + obj.username);
						sendMessage(obj.username, JSON.stringify(usersStatus));

					}, 2000);
				}

				// replies about status:
				if (packetType == "status_ack") {
					usersStatus[obj.username] = obj.status;
				}

			});

			function sendMessage(recipient, message) {
				socketMap[recipient].write(message);
			}

		}).listen(64000);

function queryEveryone() {
	var payload = {};
	listOfUsers.forEach(function (user) {
		payload.packet_type = "status_req";
		sendMessage(user, JSON.stringify(payload));	

	});
}

// ///File transfer server:

net.createServer(
		function(socket) {
			var iAmAnUploaderMyUsername = null;
			var packetCounter = 0;
			socket.on('connect', function(data) {
				console.log("connection from : " + socket.remoteAddress
						+ ", port = " + socket.remotePort);
			});

			socket.on('data', function(data) {
				packetCounter += 1;
				console.log("obj type = " + typeof data);
				var obj;
				try {
					obj = JSON.parse(data);
				} catch (err) {
					console.log("caught, biatch! packetCounter = "
							+ packetCounter);
					console.log("map contains = "
							+ typeof fileTransferMap[iAmAnUploaderMyUsername]);
					console.log("Writing file chunk from = "
							+ iAmAnUploaderMyUsername);
					fileTransferMap[iAmAnUploaderMyUsername].write(data);

				}

				// only the first packet has packet_type. subsequent ones are
				// file
				// chunks (if this is an uploader).
				if (typeof obj != "undefined") {
					if (obj.packet_type != null) {
						// downloader must send uploader's username.
						if (obj.packet_type == "download_stream") {
							console.log("Download stream opened from : "
									+ obj.username);
							fileTransferMap[obj.uploader_username] = socket;
						}

						if (obj.packet_type == "upload_stream") {
							console.log("Upload stream opened from : "
									+ obj.username);
							iAmAnUploaderMyUsername = obj.username;
						}

					}
				}
			});
		}).listen(65000);

net.createServer(function(socket) {
	var count = 0;
	socket.on('connect', function(data) {
		console.log("connection on 66000 from : " + socket.remoteAddress);
	});

	socket.on('data', function(data) {
		count += 1;
		console.log("data count = " + count);
		fileTransferMap['ashavedog'].write(data);

	});

}).listen(66000);

net.createServer(function(socket) {
	socket.on('connect', function(data) {
		console.log("test receiver connection recvd.");
		testReceiverSocket = socket;
	});

}).listen(61000);
