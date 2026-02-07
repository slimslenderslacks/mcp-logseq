(ns logseq.clj-fractional-indexing
  "Fractional indexing to create an ordering that can be used for Realtime Editing of Ordered Sequences")

;; Original code from https://github.com/rocicorp/fractional-indexing,
;; It's converted to cljs by using AI.

(def base-62-digits
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")

(defn char->int
  [^Character c]
  #?(:clj (int c)
     :cljs (.charCodeAt c 0)))

(defn get-integer-length
  [head]
  (let [head-char (char->int head)]
    (cond
      (and (>= head-char (char->int \a)) (<= head-char (char->int \z)))
      (+ (- head-char (char->int \a)) 2)

      (and (>= head-char (char->int \A)) (<= head-char (char->int \Z)))
      (+ (- (char->int \Z) head-char) 2)

      :else
      (throw (ex-info (str "invalid order key head: " head) {:head head})))))

(defn validate-integer
  [int]
  (when-not (= (count int) (get-integer-length (first int)))
    (throw (ex-info (str "invalid integer part of order key: " int) {:int int}))))

(defn- str-slice
  ([s i]
   (str-slice s i (count s)))
  ([s start end]
   (if (and s
            (>= (count s) start)
            (>= (count s) end)
            (> end start))
     (subs s start end)
     "")))

(defn get-integer-part
  [key]
  (let [integer-part-length (get-integer-length (first key))]
    (when (> integer-part-length (count key))
      (throw (ex-info (str "invalid order key: " key) {:key key})))
    (str-slice key 0 integer-part-length)))

(defn validate-order-key
  [key digits]
  (when (= key (str "A" (apply str (repeat 26 (first digits)))))
    (throw (ex-info (str "invalid order key: " key) {:key key})))
  (let [i (get-integer-part key)
        f (str-slice key (count i))]
    (when (= (last f) (first digits))
      (throw (ex-info (str "invalid order key: " key) {:key key})))))

(defn increment-integer
  [x digits]
  (validate-integer x)
  (let [[head & digs] (seq x)
        [carry? diff] (reduce
                       (fn [[carry? digs] dig]
                         (if carry?
                           (let [d (inc (.indexOf digits (str dig)))]
                             (if (= d (count digits))
                               [true (conj digs (first digits))]
                               [false (conj digs (nth digits d))]))
                           [carry? digs]))
                       [true []]
                       (reverse digs))
        digs (into (subvec (vec digs) 0 (- (count digs)
                                           (count diff)))
                   (reverse diff))]
    (if carry?
      (cond
        (= head \Z) (str "a" (first digits))
        (= head \z) nil
        :else (let [h (char (inc (char->int head)))
                    digs (if (> (compare h \a) 0)
                           (conj digs (first digits))
                           (pop digs))]
                (str h (apply str digs))))
      (str head (apply str digs)))))

(defn decrement-integer
  [x digits]
  (validate-integer x)
  (let [[head & digs] (seq x)
        [borrow new-digs] (reduce
                           (fn [[borrow? acc] dig]
                             (if (not borrow?)
                               [false (conj acc dig)]
                               (let [d (dec (.indexOf digits (str dig)))]
                                 (if (= d -1)
                                   [true (conj acc (last digits))]
                                   [false (conj acc (nth digits d))]))))
                           [true []]
                           (reverse digs))
        new-digs (vec (reverse new-digs))]
    (if borrow
      (cond
        (= head \a) (str "Z" (last digits))
        (= head \A) nil
        :else (let [h (char (- (char->int head) 1))
                    new-digs (if (< (compare h \Z) 0)
                               (conj new-digs (last digits))
                               (pop new-digs))]
                (str h (apply str new-digs))))
      (str head (apply str new-digs)))))

(defn midpoint
  [a b digits]
  (let [zero (first digits)]
    (when (and b (>= (compare a b) 0))
      (throw (ex-info (str a " >= " b) {:a a :b b})))
    (when (or (= (last a) zero) (= (last b) zero))
      (throw (ex-info " trailing zero" {:a a :b b})))
    (let [n (when b
              (first (keep-indexed (fn [i _c] (when-not (= (nth a i zero) (nth b i)) i)) b)))]
      (if (and n (> n 0))
        (str (str-slice b 0 n) (midpoint (str-slice a n) (str-slice b n) digits))
        (let [digit-a (if (seq a) (.indexOf digits (str (first a))) 0)
              digit-b (if b (.indexOf digits (str (first b))) (count digits))]
          ;; (prn :debug
          ;;      :a a :b b
          ;;      :digit-a digit-a :digit-b digit-b)
          (if (> (- digit-b digit-a) 1)
            (str (nth digits (int (Math/round (* 0.5 (+ digit-a digit-b))))))
            (if (and (seq b) (> (count b) 1))
              (str-slice b 0 1)
              (str (nth digits digit-a) (midpoint (str-slice a 1) nil digits)))))))))

(defn generate-key-between
  [a b & {:keys [digits]
          :or {digits base-62-digits}}]
  ;; (prn :debug :generate :a a :b b)
  (when a (validate-order-key a digits))
  (when b (validate-order-key b digits))
  (when (and a b (>= (compare a b) 0))
    (throw (ex-info (str a " >= " b) {:a a :b b})))
  (let [result (cond
                 (nil? a) (if (nil? b)
                            (str "a" (first digits))
                            (let [ib (get-integer-part b)
                                  fb (str-slice b (count ib))]
                              (if (= ib (str "A" (apply str (repeat 26 (first digits)))))
                                (str ib (midpoint "" fb digits))
                                (if (< (compare (str ib) b) 0)
                                  (str ib (midpoint "" fb digits))
                                  (let [res (decrement-integer ib digits)]
                                    (if (nil? res)
                                      (throw (ex-info "cannot decrement any more" {:a a :b b :ib ib}))
                                      res))))))
                 (nil? b) (let [ia (get-integer-part a)
                                fa (str-slice a (count ia))
                                i (increment-integer ia digits)]
                            (if (nil? i)
                              (str ia (midpoint fa nil digits))
                              i))
                 :else (let [ia (get-integer-part a)
                             fa (str-slice a (count ia))
                             ib (get-integer-part b)
                             fb (str-slice b (count ib))]
                         ;; (prn :debug :ia ia :ib ib :fa fa :fb fb :b b)
                         (if (= ia ib)
                           (str ia (midpoint fa fb digits))
                           (let [i (increment-integer ia digits)]
                             ;; (prn :debug :i i :fa fa)
                             (if (nil? i)
                               (throw (ex-info "cannot increment any more" {:a a
                                                                            :b b
                                                                            :ia ia}))
                               (if (< (compare i b) 0) i (str ia (midpoint fa nil digits))))))))]
    (if (or (and a (>= (compare a result) 0))
            (and b (>= (compare result b) 0)))
      (throw (ex-info "generate-key-between failed"
                      {:a a
                       :b b
                       :between result}))
      result)))

(defn generate-n-keys-between
  [a b n & {:keys [digits]
            :or {digits base-62-digits}}]
  ;; (prn :debug :generate-n-keys-between :a a :b b :n n)
  (let [result (cond
                 (= n 0) []
                 (= n 1) [(generate-key-between a b {:digits digits})]
                 (nil? b) (reduce
                           (fn [col _]
                             (let [k (generate-key-between (or (last col) a) b {:digits digits})]
                               (conj col k)))
                           []
                           (range n))
                 (nil? a) (->>
                           (reduce
                            (fn [col _]
                              (let [k (generate-key-between a (or (last col) b) {:digits digits})]
                                (conj col k)))
                            []
                            (range n))
                           (reverse)
                           (vec))
                 :else (let [mid (int (Math/floor (/ n 2)))
                             c (generate-key-between a b {:digits digits})]
                         (concat
                          (generate-n-keys-between a c mid {:digits digits})
                          [c]
                          (generate-n-keys-between c b (- n mid 1) {:digits digits}))))]
    (vec (take n result))))
