(ns games.events
 (:require
  [reagent.core :as reagent]
  [re-frame.core :as rf]
  [games.tetris.db :as tetris.db]
  [games.tetris.core :as tetris]
  [games.tetris.events :as tetris.events]
  [games.db :as db]
  [games.events.timeout]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
 ::init-db
 (fn [db] db/initial-db))
