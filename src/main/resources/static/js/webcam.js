const video = document.getElementById('videoElement');
const canvas = document.getElementById('canvas');
const context = canvas.getContext('2d');

navigator.mediaDevices.getUserMedia({ video: true })
    .then(function(stream) {
        video.srcObject = stream;
    })
    .catch(function(error) {
        console.log("Something went wrong!");
    });

function takePicture() {
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.style.display = 'block'; // Show the canvas with the photo
    sendPhotoToServer(canvas.toDataURL('image/jpeg'));
}

function sendPhotoToServer(dataUrl) {
    fetch('http://localhost:8080/api/capture', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ image: dataUrl })
//        body: JSON.stringify({ image: canvas.toDataURL('image/jpeg') })
    })
        .then(response => response.json())
        .then(data => {
            console.log('Success:', data);
            displayImageOnCanvas(data.image);
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function displayImageOnCanvas(base64Image) {
    const detectedCanvas = document.getElementById('detected-canvas');
    const detectedContext = canvas.getContext('2d');
    const detectedImage = new Image();

    detectedImage.onload = function() {
        detectedCanvas.width = detectedImage.width;
        detectedCanvas.height = detectedImage.height;
        detectedContext.clearRect(0, 0, canvas.width, canvas.height);
        detectedContext.drawImage(detectedImage, 0, 0, canvas.width, canvas.height);
        detectedCanvas.style.display = 'block'; // Show the canvas with the photo
    };

    detectedImage.src = "data:image/jpeg;charset=utf-8;base64," + base64Image;
}