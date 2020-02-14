(ns games.pause.core
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]))

(defn make-timer [id]
  {:id               id
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
  [{:keys
    [game-map-key
     n ;; TODO might need a game-name too, not just a namespace
     timers]}]
  (let [pause-evt      (keyword n :pause-game)
        resume-evt     (keyword n :resume-game)
        toggle-evt     (keyword n :toggle-pause)
        game-timer-evt (keyword n :game-timer)
        timers         (conj timers (make-timer game-timer-evt))]

    ;; pauses, ignoring whatever the current state is
    (rf/reg-event-fx
      pause-evt
      [(game-db-interceptor game-map-key)]
      (fn [{:keys [db]} _game-opts]
        {:db             (assoc db :paused? true)
         :clear-timeouts timers}))

    ;; resumes the game
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
      ;; NOTE that events coming from keybds have extra event args,
      ;; so the interceptor passes it as a list rather than game-opts directly
      (fn [{:keys [db]} game-opts]
        (if-not (:gameover? db)
          (if (:paused? db)
            ;; unpause
            {:dispatch [resume-evt game-opts]}
            ;; pause
            {:dispatch [pause-evt game-opts]}))))

    ;; TODO appears broken for tetris page game
    (rf/reg-event-fx
      game-timer-evt
      [(game-db-interceptor game-map-key)]
      (fn [{:keys [db]} {:keys [name] :as game-opts}]
        (let [{:keys [timer-increment]} db
              timer-increment           (or timer-increment 400)]
          {:db (update db :time #(+ % timer-increment))
           :timeout
           ;; NOTE depends on game-opts :name here
           {:id    (str game-timer-evt name)
            :event [game-timer-evt game-opts]
            :time  timer-increment}})))))

(comment
  (keyword :games.puyo :pause))
