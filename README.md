# vaccinale-ms

<b>Recap controlli inserimento di una somministrazione:</b>

- Spostare il regime di erogazione come primo campo da selezionare nell’inserimento di una somministrazione (campana);
- Selezione della data di somministrazione;
  - Se la data di somministrazione è successiva o uguale al 01/10/2023:
    - Se in carico SSN, oppure in compartecipazione: selezione a  scelta tra quelli disponibili a sistema (tendina) farmaco (minsan), lotto e data scadenza obbligatori;
    - Altrimenti (No SSN): inserimento manuale farmaco (minsan e denominazione farmaco), lotto e data scadenza obbligatori. Se minsan inizia con E, deve corrispondere a un farmaco estero altrimenti exception;
  - Se la data di somministrazione è antecedente al 01/10/2023 e successiva al 01/07/2019:
    - Se in carico SSN, oppure in compartecipazione: farmaco (minsan), lotto e data scadenza obbligatori a scelta tra quelli disponibili a sistema oppure inserimento manuale [prevale il disponibile sul farmaco lotto (controllo backend se inserisce un farmaco e lotto disponibile dare exception)], in questo caso valorizzare farmaco (minsan e denominazione farmaco), lotto e data scadenza obbligatori. Se minsan inizia con E, deve corrispondere a un farmaco estero altrimenti exception;
    - Altrimenti (No SSN): inserimento manuale farmaco (minsan e denominazione farmaco), lotto e data scadenza obbligatori. Se minsan inizia con E, deve corrispondere a un farmaco estero altrimenti exception;
  - Se la data di somministrazione è antecedente al 01/07/2019, selezionare regime di erogazione (vedi issue jira PIAT-271), continuiamo a gestirla attraverso la modale fuori regione/estero e campane ante 2019, inserita in questa sezione per chiudere il giro delle campane:
    - inserimento manuale farmaco (minsan facoltativo e denominazione farmaco obbligatorio se il minsan è non valorizzato), lotto e scadenza facoltativi non obbligatorio;
- In alternativa non selezionando il regime di erogazione ma uno dei link per accedere all’inserimento manuale per estero/fuori regione.
    - Selezionare regime di erogazione (vedi issue jira PIAT-271);
    - Se la vaccinazione è successiva al 01/07/2019:
      - Se vaccinazione fuori regione: inserimento manuale farmaco (minsan e denominazione farmaco) obbligatori, lotto e data scadenza facoltativi non obbligatori. Se minsan inizia con E, deve corrispondere a un farmaco estero altrimenti exception;
      - Se estera: inserimento manuale farmaco (minsan, denominazione farmaco), lotto e data scadenza facoltativi non obbligatorio;
    - Se la vaccinazione è antecedente al 01/07/2019:
      - Se vaccinazione fuori regione: inserimento manuale farmaco (minsan facoltativo e denominazione farmaco obbligatorio se il minsan è non valorizzato), lotto e scadenza facoltativi non obbligatorio;
      - Se vaccinazione estera: inserimento manuale farmaco (minsan, denominazione farmaco), lotto e data scadenza facoltativi non obbligatorio;

<b>Extra:</b> 

- Negli inserimenti da tendina e non di un farmaco, vengono effettuati i controlli di congruenza tra data somministrazione e data scadenza lotto.

- Per tutti i vaccini ad eccezione del covid, le farmacie possono registrare vaccinazioni per pazienti al di fuori della proprio asl solo attraverso il regime di erogazione a pagamento:

  - Modificato dto di input dell’endpoint che recupera le formulazioni (/vaccinale/formulazione/v1/find) FormulazioneRequestEstesoDTO.

  - Contiene gli stessi campi di prima, ma in aggiunta ha anche un boolean filtroAggiuntivoFarmacia , da valorizzare nel seguente modo:

     - In fase di prenotazione e mancata somministrazione sempre false, perché non deve applicare il filtro aggiuntivo che facciamo per le farmacie; 
    
     - In fase di somministrazione in base al regime di erogazione scelto, passare true se si è scelto (SSN/Compatecipazione), false se si è scelto NO SSN (in carico all’assistito);

- Controlli: se no ssn, e trovo farmaco ssn e lotto presenti diamo errori, se trovo solo farmaco ok andiamo oltre