
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
    const imageList = document.getElementById("image-list");
    imageList.innerHTML="";
//    const br = document.createElement("br");
    result.forEach(item => {
        const wrapperDiv = document.createElement("div");
        const divImage = document.createElement("div");
        const divButton = document.createElement("div");

        const img = document.createElement("img");
        img.src = "data:image/jpeg;charset=utf-8;base64," + item.image;
        img.alt = item.id;
//        img.width = item.width;
        img.height = "250";

        const button = document.createElement("button");
        button.textContent = "Look alike...";
        button.onclick = function() {
            lookAlike(item.id); // Associate function with parameter
        };

        divImage.appendChild(img);
        divButton.appendChild(button);

        wrapperDiv.appendChild(divImage);
        wrapperDiv.appendChild(divButton);

        imageList.appendChild(wrapperDiv);
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

function showLookAlike(similarImages) {
    const imageDisplay = document.getElementById("image-display");
    imageDisplay.innerHTML="";

    similarImages.forEach(similarImage => {
        const wrapperDiv = document.createElement("div");
        const divImage = document.createElement("div");
        const divLabel = document.createElement("div");

        const lookAlikeImage = document.createElement("img");
        lookAlikeImage.src = "data:image/jpeg;charset=utf-8;base64," + similarImage.image;
        lookAlikeImage.alt = similarImage.name;
        //lookAlikeImage.width = similarImage.width;
        //lookAlikeImage.height = similarImage.height;
        lookAlikeImage.height = "250";
//        imageDisplay.appendChild(lookAlikeImage);
        const paragraph = document.createElement("p");
        paragraph.innerHTML = similarImage.name + " - Score: " + similarImage.id;
//        imageDisplay.appendChild(paragraph);

        divImage.appendChild(lookAlikeImage);
        divLabel.appendChild(paragraph);

        wrapperDiv.appendChild(divImage);
        wrapperDiv.appendChild(divLabel);

        imageDisplay.appendChild(wrapperDiv);

    });
}

loadResults();