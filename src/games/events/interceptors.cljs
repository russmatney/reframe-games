(ns games.events.interceptors
  (:require
   [re-frame.core :as rf]
   [re-frame.interceptor :as rfi]
   [re-frame.utils :as rfu]
   [adzerk.cljs-console :as log]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Intereceptors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO break event modifier out of game-db path usage
;; one for list event -> map or subvec 1 list
;; one for pulling key out of game-opts
;; TODO accept fn for getting k out of game-opts (instead of implicit :name)
(defn game-db-interceptor
  "Interceptor that operates over the game db.
  Events passed through this interceptor will
  have their first argument consumed and used to
  find the game-db passed into the handler.

  The event will also be trimmed. Whether this helps
  or causes more insanity remains an open question."
  []
  (rfi/->interceptor
    :id :game-db-interceptor
    :before
    (fn game-db-interceptor-before
      [context]
      (let [event (get-in context [:coeffects :event])
            {:keys [name]}
            (if (> (count event) 1) (nth event 1)
                (log/warn
                  "Game-interceptor received event without argument: ~{event}"))]
        (-> context
            ;; set db to the game's db
            (assoc-in
              [:coeffects :db]
              (get-in context [:coeffects :db :games name]))

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
                         (assoc-in og-db [:games name] game-db)
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
  [(game-db-interceptor)]
  (fn [{:keys [db]} _game-opts]
    (log/debug "~{_game-opts}")
    (log/debug "~{(keys db)}")
    {:db db}))


(comment
  (rf/dispatch [::interceptor-example {:name :default}]))

(defn ->fancy-interceptor
  "'This thing is just a map.'"
  [& {:keys [id before after]}]
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
