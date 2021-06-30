(ns crayon.core
  (:require
   [clojure.string :as str]))

(def reset-code "\033[0m")
(def fg-reset-code "\033[39m")
(def bg-reset-code "\033[49m")

(def color-map {:black 0
                :red 1
                :green 2
                :yellow 3
                :blue 4
                :magenta 5
                :cyan 6
                :white 7})

;; TODO: check ANSI_COLORS_DISABLED

(def attr-map {:bold 1
               :italic 3
               :underline 4
               :strike 9})

(defn rgb
  [r g b]
  (format "\033[38;2;%d;%d;%dm" r g b))

(defn bg-rgb
  [r g b]
  (format "\033[48;2;%d;%d;%dm" r g b))

(defn hex*
  [hex-symbol]
  (let [s (subs (name hex-symbol) 1)]
    (map
     #(Integer/parseInt (apply str %) 16)
     (partition 2 s))))

(defn hex
  [hex-symbol] ;; should start with "#"
  (let [[r g b] (hex* hex-symbol)]
    (format "\033[38;2;%d;%d;%dm" r g b)))

(defn bg-hex
  [hex-symbol] ;; should start with "#"
  (let [[r g b] (hex* hex-symbol)]
    (format "\033[48;2;%d;%d;%dm" r g b)))

(defn colorize
  [result-seq]
  (str
   (->> result-seq
        (reduce
         (fn [acc style-and-str]
           (let [[last-style acc-seq] acc
                 [style s] style-and-str
                 merged (merge last-style style)
                 {:keys [fg bg attr]} merged
                 attrs (when (seq attr)
                         (map attr-map attr))]
             [merged
              (conj acc-seq
                    (str
                     (cond->> s
                       bg (format "%s%s" (cond
                                           (bg color-map) (format "\033[48;5;%dm" (bg color-map))
                                           (= :none bg) bg-reset-code
                                           :else (bg-hex bg)))
                       fg (format "%s%s" (cond
                                           (fg color-map) (format "\033[38;5;%dm" (fg color-map))
                                           (= :none fg) fg-reset-code
                                           :else (hex fg)))
                       attrs (format "\033[%sm%s" (str/join ";" attrs)))))]))
         [{} []])
        second
        (apply str))
   reset-code))

(defn update-style
  [style-map sym]
  (cond
    (= sym :fg-reset) (assoc style-map :fg :none)
    (= sym :bg-reset) (assoc style-map :bg :none)
    (= sym :reset) {:fg :none
                    :bg :none
                    :attr []}

    (str/starts-with? (name sym) "bg-")
    (assoc style-map :bg (-> (name sym)
                             str/lower-case
                             (subs 3)
                             keyword))

    (sym attr-map)
    (update style-map :attr #(conj % sym))

    :else
    (assoc style-map :fg sym)))

(defn parse-seq
  [in-seq]
  (reduce
   (fn [acc arg]
     (condp #(%1 %2) arg
       string? (update acc :result #(conj % [(:style acc) arg]))
       keyword? (update acc :style #(update-style % arg))
       vector? (update acc :style #(merge % (:style (parse-seq arg))))
       :default))
   {:style {:attr []}
    :result []}
   in-seq))

(defn =>
  [& args]
  (colorize
   (:result (parse-seq args))))

