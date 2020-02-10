(ns games.events.interceptors
  (:require
   [re-frame.core :as rf]
   [re-frame.interceptor :as rfi]
   [re-frame.utils :as rfu]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Intereceptors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn game-db-interceptor
  "Interceptor that operates over the game db.
  Events passed through this interceptor will
  have their first argument consumed and used to
  find the game-db passed into the handler.

  The event will also be trimmed. Whether this helps
  or causes more insanity remains an open question."
  [game-key]
  (rfi/->interceptor
    :id :game-db-interceptor
    :before
    (fn game-db-interceptor-before
      [context]
      (let [{:keys [name]} (nth (get-in context [:coeffects :event]) 1)]
        (-> context
            ;; set db to the game's db
            (assoc-in
              [:coeffects :db]
              (get-in context [:coeffects :db game-key name]))

            ;; set original-db for :after clause
            (assoc-in
              [:coeffects ::original-db]
              (get-in context [:coeffects :db]))

            ;; trim event object
            (update-in [:coeffects :event]
                       (fn [event]
                         (let [trimmed (subvec event 1)]
                           (if (= 1 (count trimmed))
                             (first trimmed)
                             trimmed))))

            ;; store untrimmed for retrieval
            (assoc-in [:coeffects ::untrimmed-event]
                      (get-in context [:coeffects :event])))))
    :after
    (fn game-db-interceptor-after
      [context]
      (let [{:keys [name]}
            (nth (get-in context [:coeffects ::untrimmed-event]) 1)
            game-db    (-> context :effects :db)
            og-db      (-> context :coeffects ::original-db)
            updated-db (if game-db
                         (assoc-in og-db [game-key name] game-db)
                         og-db)]
        (-> context
            ;; clean up trimming, retore event
            (rfu/dissoc-in [:coeffects ::untrimmed-event])
            (assoc-in [:coeffects :event]
                      (get-in context [:coeffects ::untrimmed-event]))
            ;; remove our helper
            (rfu/dissoc-in [:coeffects ::original-db])
            ;; set the new db on 'EFFECTS' (NOT COEFFECTS)
            (assoc-in [:effects :db] updated-db))))))


(rf/reg-event-fx
  ::interceptor-example
  [(game-db-interceptor :games.puyo.db/db)]
  (fn [{:keys [db]} _game-opts]
    ;; (println game-opts)
    ;; (println (keys db))
    {:db db}))


(comment
  (rf/dispatch [::interceptor-example {:name :default}]))

(defn ->fancy-interceptor
  "'This thing is just a map.'"
  [& {:keys [id before after]}]
  (when true (println "using the fancy interceptor! Good luck!"))
  (rfi/->interceptor
    :id     (or id :unnamed)
    :before before
    :after  after )
  )

(comment
  (def my-fancy-interceptor
    (->fancy-interceptor
      :id :something-fancy
      :before
      (fn remove-complexity [ctx] ctx)
      :after
      (fn add-back-to-context
        ;; Remember to add to :effects, not :coeffects
        [ctx] ctx))))
