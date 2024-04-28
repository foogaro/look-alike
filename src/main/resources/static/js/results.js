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
    const imageList = document.querySelector(".image-list");
    imageList.innerHTML="";

    const carouselTrack = document.querySelector('.carousel-track');
    carouselTrack.innerHTML = '';

    result.forEach((img, idx) => {

        const listItemElement = document.createElement('li');
        const imgElement = document.createElement('img');
        imgElement.src = "data:image/jpeg;charset=utf-8;base64," + img.image;
        imgElement.alt = img.id;
        imgElement.height = "250";
        imgElement.onclick = () => {
            lookAlike(img.id);
        };
        listItemElement.appendChild(imgElement)
        imageList.appendChild(listItemElement);
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
    const carouselTrack = document.querySelector('.carousel-track');
    carouselTrack.innerHTML='';
    const imageListItems = document.querySelectorAll('.image-list img'); // Seleziona tutte le immagini a sinistra

    similarImages.forEach((similarImage, idx) => {

        const imgElement = document.createElement('img');
        imgElement.src = "data:image/jpeg;charset=utf-8;base64," + similarImage.image;
        imgElement.alt = similarImage.name + ' - Score: ' + similarImage.id;
        imgElement.height = "250";
        imgElement.onclick = () => {
            updateDescription(similarImage.name + ' - Score: ' + similarImage.id);
            updateActiveImage(idx, carouselTrack);
        };
        carouselTrack.appendChild(imgElement);
    });

    // Rimuovi la classe selected da tutte le immagini a sinistra e aggiungila a quella cliccata
    function highlightSelectedImage(selectedImg) {
        imageListItems.forEach(img => img.classList.remove('selected'));
        selectedImg.classList.add('selected');
    }

    // Imposta la descrizione e l'immagine attiva per la prima immagine
    updateDescription(carouselTrack.firstChild.alt);
    updateActiveImage(0, carouselTrack); // Evidenzia la prima immagine all'inizio

/*
    // Aggiungi l'evento di clic alle immagini di sinistra
    imageListItems.forEach((img, idx) => {
        img.onclick = () => {
            highlightSelectedImage(img);
            updateDescription(img.name + ' - Score: ' + img.id);
        };
    });
*/

}


function updateActiveImage(index, imagesContainer) {
    const images = imagesContainer.querySelectorAll('img');
    images.forEach((img, idx) => {
        if (idx === index) {
            img.classList.add('active');
        } else {
            img.classList.remove('active');
        }
    });
}

function updateDescription(imgDescription) {
    document.querySelector('.image-description').textContent = imgDescription;
}

loadResults();
