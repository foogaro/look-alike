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
    video.style.display = 'none'; // Show the canvas with the photo
    document.getElementById('overlay-bottom-right-text-clear').style.display = 'block';
    document.getElementById('overlay-bottom-right-text').style.display = 'none';
    document.getElementById('overlay-top-right-text-clear').style.display = 'block';
    document.getElementById('overlay-top-right-text').style.display = 'none';
    document.getElementById('show-results').style.display = 'inline';
    sendPhotoToServer(canvas.toDataURL('image/jpeg'));
}

function clearPicture() {
    document.getElementById('overlay-bottom-right-text-clear').style.display = 'none';
    document.getElementById('overlay-bottom-right-text').style.display = 'block';
    document.getElementById('overlay-top-right-text-clear').style.display = 'none';
    document.getElementById('overlay-top-right-text').style.display = 'block';
    document.getElementById('show-results').style.display = 'none';
    canvas.style.display = 'none';
    video.style.display = 'block';
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

function loadResults() {
    fetch('http://localhost:8080/api/results', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    })
        .then(response => response.json())
        .then(data => {
            console.log('Success:', data);
            viewResults(data);
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function viewResults(result) {
    const container = document.getElementById("container");
    result.forEach(item => {
        // Create image element
        const img = document.createElement("img");
        img.src = "data:image/jpeg;charset=utf-8;base64," + item.image;
        img.alt = item.id;
        img.width = item.width
        img.height = item.height

        // Create button element
        const button = document.createElement("button");
        button.textContent = "Click Me";
        button.onclick = function() {
            lookAlike(item.id); // Associate function with parameter
        };

        // Append elements to the container
        container.appendChild(img);
        container.appendChild(button);
    });
}

function lookAlike(param) {
    fetch('http://localhost:8080/api/look-alike/' + param, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    })
        .then(response => response.json())
        .then(data => {
            console.log('Success:', data);
            showLookAlike(data);
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function showLookAlike(similarImage) {
    const container = document.getElementById("container");
    const lookAlikeImage = document.createElement("img");
    lookAlikeImage.src = "data:image/jpeg;charset=utf-8;base64," + similarImage.image;
    lookAlikeImage.alt = similarImage.name;
    lookAlikeImage.width = similarImage.width;
    lookAlikeImage.height = similarImage.height;
    container.appendChild(lookAlikeImage);
    const paragraph = document.createElement("p");
    paragraph.innerHTML = similarImage.name + " - " + similarImage.id;
    container.appendChild(paragraph);
}