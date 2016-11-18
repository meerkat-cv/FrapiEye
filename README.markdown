# frAPI Eye

This is an Android project that encodes the video from the cellphone cameras in h264 and transmits through websocket to our on premise [facial recognition API][frapi]. This enables our frAPI users to use the cellphone as a IP Camera while also geting the recognition and face detection results presented directly on the phone.

For the video encode we used from [android-h264-stream-demo][demo_site], which is quite small and simple to use. And for the websocket connection we used [nv-websocket-client][websocket], which seems quite robust and very simple to set up.

Eventhough this is an app to complement our on premise facial recognition, the code to encode an h264 video and send through websockets might be useful for you. And if you want to decode this video stream, you can checkout our small C++ lib (with a python3 wrapper) to decode in [here][h264_decoder]. It quite small, a really simple interface and no external dependencies, so it is quite easy to include in your project.

Hope you like it :)

[Meerkat team][Meerkat_site]


### Contribution

This project started with our other Android project, [ava-preview][ava] as base, so there may be still some code that doesn't quite fit in here that is from there. With time we plan to clean this code.
If you find a bug/problem please open an Issue, so that everyone can be aware of. 


[demo_site]: <https://github.com/bytestar/android-h264-stream-demo>
[websocket]: <https://github.com/TakahikoKawasaki/nv-websocket-client>
[frapi]: <http://www.meerkat.com.br/en/solution_facial_recognition.html>
[h264_decoder]: <https://github.com/meerkat-cv/h264_decoder>
[Meerkat_site]: <http://www.meerkat.com.br/?setLng=en-US>
[ava]: <https://github.com/meerkat-cv/ava_preview>