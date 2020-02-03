(ns games.sound
  (:require
   [cljs.core.async :refer [<! chan put! close!]]
   [re-frame.core :as rf :refer [subscribe dispatch]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defn load-sound [named-url]
  (let [out (chan)
        req (js/XMLHttpRequest.)]
    (set! (.-responseType req) "arraybuffer")
    (set! (.-onload req) (fn [_e]
                           (if (= (.-status req) 200)
                             (do (put! out (assoc named-url :buffer (.-response req)))
                                 (close! out))
                             (close! out))))
    (.open req "GET" (:url named-url) true)
    (.send req)
    out))

(defn decode [named-url]
  (let [out (chan)]
    (if (:buffer named-url)
        (.decodeAudioData
         @(subscribe [:audio-context]) (:buffer named-url)
         (fn [decoded-buffer]
           (put! out (assoc named-url :decoded-buffer decoded-buffer))
           (close! out))
         (fn []
           (.error js/console "Error loading file " (prn named-url))
           (close! out)))
      (close! out))
    out))

(defn get-and-decode [named-url]
  (go
    (when-let [s (<! (load-sound named-url))]
      (<! (decode s)))))

(defn load-samples []
  (go-loop [result {}
            sounds (range 1 2)]
    (if-not (nil? (first sounds))
      (let [sound          (first sounds)
            decoded-buffer (<! (get-and-decode {:url   (str "/audio/" sound ".mp3")
                                                :sound sound}))]
        (prn sound)
        (prn decoded-buffer)
        (recur (assoc result sound decoded-buffer)
               (rest sounds)))
      result)))

(def loading-samples
  (go
    (dispatch [:load-samples  (<! (load-samples))])
    (prn "Samples loaded")
    (dispatch [:play-sample 1])))

(defn play-sample! [sample]
  (let [context       (subscribe [:audio-context])
        samples       (subscribe [:samples])
        audio-buffer  (:decoded-buffer (get @samples sample))
        sample-source (.createBufferSource @context)]
    (set! (.-buffer sample-source) audio-buffer)
    (.connect sample-source (.-destination @context))
    (.start sample-source)
    sample-source))

(comment
  (dispatch [:play-sample 1])
  @(subscribe [:samples])
  )