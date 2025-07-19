(ns jdbc-to-json.core
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [jdbc-to-json.logging :as logging])
  (:import [java.io File])
  (:gen-class))

(def cli-options
  [["-h" "--host HOST" "Database host"
    :default "localhost"]
   ["-p" "--port PORT" "Database port"
    :default 5432
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--database DATABASE" "Database name"
    :required true]
   ["-u" "--username USERNAME" "Database username"
    :required true]
   ["-w" "--password PASSWORD" "Database password (will prompt if not provided)"]
   ["-s" "--schema SCHEMA" "Database schema"
    :default "public"]
   ["-o" "--output-dir OUTPUT_DIR" "Output directory for JSONL files"
    :default "./output"]
   ])

(defn usage [options-summary]
  (->> ["PostgreSQL to JSON Export Tool"
        ""
        "Usage: clojure -M:run [options]"
        ""
        "Options:"
        options-summary
        ""
        "Example:"
        "  clojure -M:run -h localhost -p 5432 -d mydb -u myuser -w mypass -s public -o ./export"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn prompt-for-password
  "Prompt user for password input without echoing to console"
  []
  (print "Enter database password: ")
  (flush)
  (let [console (System/console)]
    (if console
      (String. (.readPassword console))
      (do
        (println "Warning: Console not available, password will be visible")
        (read-line)))))

(defn validate-args [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      
      :else ; success => process options
      {:options options})))

(defn create-db-spec [options]
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname (str "//" (:host options) ":" (:port options) "/" (:database options))
   :user (:username options)
   :password (:password options)})

(defn get-table-names "Get all table names from the specified schema"
  [db-spec schema]
  
  (try
    (let [query "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'"]
      (->> (jdbc/query db-spec [query schema])
           (map :table_name)))
    (catch Exception e
      (log/error e "Failed to get table names" :schema schema :error (.getMessage e))
      (throw e))))

(defn export-table-to-json
  "Export a single table to JSONL using PostgreSQL's row_to_json function"
  [db-spec schema table-name output-dir]
  
  (try
    (let [query (str "SELECT row_to_json(t) as json_data FROM " schema "." table-name " t")
          results (jdbc/query db-spec query)
          json-strings (map :json_data results)
          output-file (io/file output-dir (str table-name ".json"))]
      
      (log/info "Exporting table" 
                :table table-name 
                :schema schema 
                :output-file (.getPath output-file)
                :query-rows (count json-strings))
      
      ;; Ensure parent directory exists
      (.mkdirs (.getParentFile output-file))
      
      ;; Write JSONL data directly from PostgreSQL
      (with-open [writer (io/writer output-file)]
        (doseq [json-str json-strings]
          (.write writer (str json-str "\n"))))
      
      (log/info "Successfully exported table" 
                :table table-name 
                :rows-exported (count json-strings)
                :file-size (.length output-file)
                :status "success")
      {:table table-name :rows (count json-strings) :success true})
    
    (catch Exception e
      (log/error e "Failed to export table" 
                 :table table-name 
                 :schema schema 
                 :error (.getMessage e)
                 :status "failed")
      {:table table-name :error (.getMessage e) :success false})))

(defn ensure-output-directory
  "Ensure the output directory exists"
  [output-dir]
  (let [dir (io/file output-dir)]
    (when-not (.exists dir)
      (log/info "Creating output directory" :directory output-dir :action "mkdir")
      (.mkdirs dir))))

(defn export-all-tables
  "Export all tables from the specified schema to JSON files"
  [options]
  
  (let [db-spec (create-db-spec options)
        schema (:schema options)
        output-dir (:output-dir options)]
    
    (log/info "Starting database export" 
              :database (:database options)
              :host (:host options)
              :port (:port options)
              :schema schema
              :output-dir output-dir
              :username (:username options))
    
    (ensure-output-directory output-dir)
    
    (try
      ;; Test database connection
      (jdbc/query db-spec "SELECT 1")
      (log/info "Database connection established" :status "connected")
      
      ;; Get all table names
      (let [table-names (get-table-names db-spec schema)]
        (if (empty? table-names)
          (do
            (log/warn "No tables found in schema" :schema schema :table-count 0)
            {:success false :message (str "No tables found in schema: " schema)})
          
          (do
            (log/info "Tables discovered" 
                      :schema schema 
                      :table-count (count table-names)
                      :tables (vec table-names))
            
            ;; Export each table
            (let [results (doall (map #(export-table-to-json db-spec schema % output-dir) table-names))
                  successful (filter :success results)
                  failed (filter #(not (:success %)) results)]
              
              (log/info "Export operation completed"
                        :total-tables (count table-names)
                        :successful-exports (count successful)
                        :failed-exports (count failed)
                        :success-rate (if (> (count table-names) 0)
                                       (float (/ (count successful) (count table-names)))
                                       0.0))
              
              (when (seq failed)
                (log/error "Some table exports failed" 
                           :failed-tables (mapv :table failed)
                           :failure-count (count failed)))
              
              {:success (empty? failed)
               :total (count table-names)
               :successful (count successful)
               :failed (count failed)
               :results results}))))
      
      (catch Exception e
        (log/error e "Database connection or query failed" 
                   :database (:database options)
                   :host (:host options)
                   :port (:port options)
                   :error (.getMessage e))
        {:success false :error (.getMessage e)}))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (logging/configure-logging!)

  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      
      (let [final-options (if (:password options)
                           options
                           (assoc options :password (prompt-for-password)))
            result (export-all-tables final-options)]
        (if (:success result)
          (do
            (System/exit 0))
          (do
            (println (str "Export failed: " (or (:error result) (:message result))))
            (System/exit 1)))))))
