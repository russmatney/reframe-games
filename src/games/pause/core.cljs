(ns games.pause.core
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]))

;; TODO this is pretty gross
(defn id->game-timer-id [id game-opts]
  (keyword id (:name game-opts)))

(defn make-timer [id]
  {:game-opts->id    (fn [gopts] (id->game-timer-id id gopts))
   :game-opts->event (fn [gopts] [id gopts])})

(defn reg-pause-events
  "Registers pause events with the passed options.

  Reads db keys:
  - :paused?
  - :gameover?

  Sets db keys:
  - :paused?
  - :time

  Supports events:
  - :nspc/pause-game
  - :nspc/resume-game
  - :nspc/toggle-pause

  Supports a game timer via:
  - event - :nspc/game-timer
  - db fields
    - :timer-increment
    - :time

  Expects `timers` like:
  `[{:id      ::game-tick
     :game-opts->event (fn [x] [::game-tick x])}]`
  "
  [{:keys [game-map-key n timers]}]
  (let [pause-evt      (keyword n :pause-game)
        resume-evt     (keyword n :resume-game)
        toggle-evt     (keyword n :toggle-pause)
        game-timer-evt (keyword n :game-timer)
        timers         (conj timers (make-timer game-timer-evt))]

    (rf/reg-event-fx
      pause-evt
      [(game-db-interceptor game-map-key)]
      (fn [{:keys [db]} game-opts]
        (let [timer-ids (map
                          (fn [timer]
                            {:id ((:game-opts->id timer) game-opts)})
                          timers)
              ]
          {:db (assoc db :paused? true)
           :clear-timeouts
           (seq timer-ids)})))

    (rf/reg-event-fx
      resume-evt
      [(game-db-interceptor game-map-key)]
      (fn [{:keys [db]} game-opts]
        (let [updated-db (assoc db :paused? false)]
          (-> {:db updated-db}
              (assoc :dispatch-n
                     (map #((:game-opts->event %) game-opts)
                          timers))))))

    (rf/reg-event-fx
      toggle-evt
      [(game-db-interceptor game-map-key)]
      (fn [{:keys [db]} game-opts]
        (if-not (:gameover? db)
          (if (:paused? db)
            ;; unpause
            {:dispatch [resume-evt game-opts]}
            ;; pause
            {:dispatch [pause-evt game-opts]}))))

    (rf/reg-event-fx
      game-timer-evt
      [(game-db-interceptor game-map-key)]
      (fn [{:keys [db]} game-opts]
        (let [{:keys [timer-increment]} db
              timer-increment           (or timer-increment 400)]
          {:db (update db :time #(+ % timer-increment))
           :timeout
           {:id    (id->game-timer-id game-timer-evt game-opts)
            :event [game-timer-evt game-opts]
            :time  timer-increment}})))))

(comment
  (keyword :games.puyo :pause))
