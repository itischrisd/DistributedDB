# DistributedDB
Distributed database application created as final project assignment for Computer Networks course on PJAIT

Opis projektu rozproszonej bazy danych z przedmiotu SKJ


# PROTOKÓŁ - STRUKTURA PAKIETU

Zrozumiały opis implementacji projektu najłatwiej zacząć od opisu protokołu zaimplementowanego na potrzeby rozproszonej bazy danych, a w szczególności od struktury komunikatu. Ułatwi to późniejsze wyjaśnienie sposobu łączenia i rozłączania węzłów, routingu pakietów wewnątrz bazy, ich przetwarzania, a co za tym idzie dostarczenia usług klientowi.

W systemie zaimplementowano dwie wersje obsługiwanych komunikatów.

Pierwszy - prosty - jest identyczny z komunikatem przesyłanym przez klienta (zgodny z wymaganiami projektowymi). Jest więc wykorzystywany do przyjęcia żądania od klientów, przesłania do nich ostatecznej odpowiedzi przez węzeł brzegowy, ale również, ze względu na swą prostotę, do informowania węzłów-sąsiadów o wyłączającym się węźle (przekazanie informacji sąsiadom o tym, że nadawca wykonał polecenie 'terminate'). Struktura pakietu, zgodna z wymaganiem projektowym w zakresie obsługi żądań klienta, wygląda następująco:

<POLE POLECENIA> <POLE CIAŁA ŻĄDANIA/ODPOWIEDZI>

Pola oddzielone są od siebie znakiem spacji. Oba pola nie są obowiązkowe, tzn. jeśli komunikat jest odpowiedzią dla klienta, to nie będzie zawierał pola polecenia (np. 'ERROR', 'OK', '127.0.0.1:9000'), a jeśli jest żądaniem typu 'terminate', nie będzie zawierał pola ciała. Poza realizacją usług dla klienta węzły wymieniają między sobą następujące komunikaty proste:
- handshake <adres nowego węzła> - nowy węzeł wysyła informację o tym, że chce przyłączyć się do istniejącego węzła i przez jaki adres chciałby się identyfikować
- OK - taki komunikat przesyła do swoich sąsiadów węzeł, wykonujący polecenie 'terminate'; jest to z punktu widzenia połączeń miedzywęzłowych komunikat unikatowy, tak więc działające węzły wiedzą, że taką wiadomość wysyłają jedynie sąsiedzi kończący swoje działanie w systemie

Drugi typ komunikatów - złożony - służy do propagacji żądań wewnątrz systemu, a także do przekazywania zwrotnej odpowiedzi. Jego struktura jest następująca:

<ID> <POLE POLECENIA> <CIAŁO ŻĄDANIA> <CIAŁO ODPOWIEDZI> <POLE HISTORII PAKIETU>

Tak jak w przypadku komunikatu prostego, poszczególne pola oddzielone są znakiem spacji. Zawierają one następujące informacje:
- ID - pozwala jednoznacznie identyfikować pakiet związany z obsługą pojedynczego, unikatowego żądania klienta wewnątrz systemy, jest nadawany przez pierwszy węzeł odbierający żądanie klienta i jest określany przez czas sytemu węzła brzegowego w sekundach; w tym miejscu warto zauważyć, że dzięki niemu możliwe jest szybkie rozróżnianie przez węzły pakietów prostych (od/dla klienta, informujących o nowym i zamykającym się węźle) od złożonych - pierwszym znakiem pakietu złożonego jest zawsze cyfra, a prostego zawsze litera; pole ID jest niezmienne przez cały cykl życia żądania; skopiowana wartość z komunikatu prostego otrzymanego od klienta, również niezmienna po jej pierwszym ustawieniu, pozwala węzłom identyfikować, które funkcje wewnątrz swojej logiki należy wykonać w ramach obsługi żądania
- ciało żądania - ostatnia z wartości niezmiennych - jeśli żądanie klienta posiadało parametr, jego wartość będzie wstawiona tutaj; w przeciwnym wypadku pole zostanie utworzone z wartością "NUL" (nie mylić z javowym null, chodzi o ciąg trzech znaków)
- ciało odpowiedzi - pole wykorzystywane do wystosowania odpowiedzi systemu na żądanie klienta; początkowo nadawana mu jest wartość "ERROR", tak by brak możliwości zrealizowania żądania przez system pozostawał wartością domyślną; gdy któryś z węzłów jest w stanie zrealizować żądanie (np udało mu się wykonać 'set-value', albo jego adres posiada odpowiedź na 'find-key x'), umieszcza w tym polu stosowne dane, nadpisując początkowy "ERROR"
- pole historii pakietu - umożliwia realizację optymalnego routingu komunikatu z żądaniem klienta; gdy pakiet o unikatowym ID pierwszy raz trafia do węzła, dopisuje on w tym polu, na jego końcu, swój adres w formacie IP:PORT (poszczególne adresy oddzielone są przecinkiem); dzięki temu każdy węzeł a) obsługuje pakiet tylko raz, oraz b) wie, którzy z jego sąsiadów obsługiwali już pakiet (do tych węzłów pakietu już nie prześle, szczegóły w sekcji ROUTING); kolejne adresy są oddzielone przecinkami, tak więc razem stanowią ciąg bez znaku spacji - w myśl projektu protokołu stanowią jedno pole


# OBSŁUGA TOPOLOGII SIECI

W największym uproszczeniu - implementacja systemu rozproszonego pozwala na realizację sieci węzłów bazodanowych o postaci dowolnego, nieskierowanego grafu. Aspekt techniczny tej immplementacji został zrealizowany następująco - każdy węzeł w momencie uruchomienia tworzy obiekt klasy ServerSocketChannel tworzący gniazdo zadane przez parametr uruchomieniowy. Gniazdo to działa w trybie non-blocking, tzn. nieustannie, tak długo jak działa węzeł, czeka na danym porcie na dane lub handshake TCP od węzła/klienta chcącego z nim nawiązać nowe połączenie. Node działa w niekończącej się pętli, za każdym razem sprawdzając, czy obiekt ServerSocketChannel zwróci nowe gniazdo (czy też null, gdy nikt nie czeka po drugiej stronie na nawiązanie połączenia TCP). Jeśli nowe gniazdo zostanie utworzone, węzeł doda je do listy obsługiwanych połączeń i sprawdzi przy każdej iteracji, czy strumień danych posiada dane przychodzące do obsłużenia, czy też null. Dodatkowo, gdy na gnieździe pakietem przychodzącym będzie komunikat prosty typu handshake, węzeł doda gniazdo do listy znanych sąsiadów, będących elementami bazy rozproszonej. W momencie, gdy węzeł stwierdzi, że gniazdo zostało zamknięte, usunie je z listy i nie będzie go więcej sprawdzał ani przekazywał dalej do niego żadnych pakietów. Testy wykazały, że samo utworzenie i zamknięcie sieci jest możliwe dla co najmniej 20 węzłów połączonych w niebanalne struktury grafowe. Dodatkowym atutem takiego rozwiązania jest fakt, że każdy węzeł posiada oddzielne połączenie ze swoim sąsiadem w dupleksie. Ta korzyść, w połączeniu z algorytmem routingu i sposobem identyfikacji unikatowych żądań, pozwoliła spełnić wymaganie projektowe nr 3.4 - równoległe obsługiwanie wielu klientów podłączonych do dowolnych węzłów brzegowych (testy wkonano dla 7 węzłów i 7 równoczesnych połączeń z klientami).


# ROUTING PAKIETÓW

Do pełnego wytłumaczenia, w jaki sposób system rozproszony przesyła pakiet z komunikatem żądania/odpowiedzi między swoimi węzłami, jest konieczna informacja o drugim miejscu, w którym przechowywane są dane o pakiecie. Pierwszym było samo pole komunikatu z historią jego wędrówki po węzłach. Drugim jest pamięć samych węzłów. W momencie, gdy pojedynczy węzeł otrzyma pakiet od klienta lub sąsiadującego węzła, sprawdzi (korzystając z pola ID), czy jest to pierwszy raz, gdy pakiet powiązany z żądaniem trafia do niego, czy jest to kolejny raz. W przypadku pierwszych "odwiedzin" pakietu węzeł dodaje do swojej pamięci dane w postaci klucz:wartość, w której unikatowym kluczem jest ID pobrane z pakietu, a wartością adres nadawcy, od którego węzeł dostał pakiet po raz pierwszy (wartość nigdy nie jest nadpisana, zawsze wskazuje na adres tego nadawcy, który był dla węzła "oryginalnym" nadawcą). Pierwsza wizyta pakietu w węźle jest też jedyną, przy której węzeł wykonuje metodę wskazaną przez pole polecenia, z argumentem ciała żądania i, jeśli to konieczne, umieszcza właściwą odpowiedź w polu ciała odpowiedzi.

Kolejnym elementem routingu pakietu jest, po obsłużeniu logiki wymaganej przez żądanie, przeiterowanie po gniazdach podłączonych sąsiadów i wysłanie pakietu do pierwszego sąsiada, którego adres nie znajduje się w polu historii komunikatu. Jeśli węzeł w oparciu o to sprawdzenie stwierdzi, że komunikat ten był już w każdym z jego sąsiadów (bo np. jedynym jego sąsiadem jest nadawca, albo pakiet właśnie pokonuje drogę powrotną do klienta), wtedy ponownie sprawdzi swoją pamięć, odszukując po kluczu równym ID z pola pakietu, wartość IP:PORT węzła, od którego dostał komunikat. Po pobraniu wartości węzeł sprawdzi, które z jego gniazd odpowiada właściwemu adresowi i do niego odeśle pakiet, który w jego sąsiedztwie z punktu widzenia żądania można uznać za obsłużone.

Dodatkową rzeczą, którą przed wysłaniem "w górę hierarchii" obsłużonego pakietu sprawdzi węzeł, jest fakt, czy aby sam nie był pierwszym adresem w historii pakietu. Jeśli tak jest, to znaczy że oryginalnym nadawcą jest w jego przypadku klient. Węzeł przed wysłaniem danych przekonwertuje komunikat złożony do komunikatu prostego, tak aby klient otrzymał te (i tylko te) dane, o które żądał.

Po krótkim omówieniu routingu i przy założeniu, że sieć stanowiąca bazę rozproszoną jest dowolnym grafem nieskierowanym, nietrudno zauważyć, że routing opiera się o implementację własną algorytmu Depth Search First (przeszukiwania grafu w głąb). Jest to implementacja, która pozwala, by w rozwiązaniu postawionego w projekcie problemu a) żądanie nie było rozwiązywane przez każdy węzeł więcej niż raz, b) żądanie trafiło do każdego węzła bazy rozproszonej, c) pakiet z żądaniem nie uległ duplikacji ani zakleszczeniu w żadnym miejscu, a także d) pakiet z odpowiedzią znalazł drogę powrotną do klienta bez koordynacji węzłów między sobą, wracając zawsze tą samą drogą, jaką przyszedł, jednak nie wchodząc w pomijalne gałęzie grafu.


# PRZETWARZANIE PAKIETÓW

Tak jak zostało to opisane w poprzednim punkcie, węzeł realizuje logikę obsługi żądania tylko dla pierwszej wizyty pakietu żądania. Bez wchodzenia w szczegóły implementacji funkcji javowych, rozproszenie obsługi poszczególnych poleceń uzyskano w następujący sposób:

- set-value - Pakiet trawersuje sieć w poszukiwaniu węzła o zadanym kluczu. Dopóki go nie odnajdzie, pole komunikatu ciała odpowiedzi posiadać będzie wartość "ERROR", nadaną w momencie utworzenia. Jeśli pakiet nie odnajdzie węzła o właściwym kluczu i powróci do węzła brzegowego, siłą rzeczy taka właśnie wartość będzie przekazana do klienta. Jeśli po drodze jeden lub więcej węzłów okaże się posiadać stosowny klucz, ustawi u siebie wartość z pola ciała żądania, do pola ciała odpowiedzi wpisze wartość "OK" i przekaże pakiet dalej. Pakiet trafi do każdego węzła i w każdym węźle o zadanym kluczu ustawiona zostanie przekazana wartość.

- get-value - Tak jak w przypadku set-value - pakiet przejdzie po całym systemie, a każdy węzeł posiadający właściwy klucz nadpisze pole ciała odpowiedzi swoim kluczem:wartością (nadpisując ERROR, jeśli był pierwszym pomyślnym dopasowaniem, lub poprzedni klucz:wartość, jeśli kolejnym).

- find-key - Identyczna implementacja, jak w przypadku get-value. Również tutaj pomyślne znalezienie klucza nie wpływa na algorytm routingu, pakiet przejdzie przez całą sieć, zwracając de facto ostatni adres IP:PORT właściciela poszukiwanego klucza.

- get-max - Pakiet żądania okrążając sieć wywołuje sprawdzenie w węźle, czy posiadana wartość jest większa od wpisanej w pole ciała odpowiedzi. Jeśli tak, nadpisze ją swoją własną. Jeśli jest pierwszym węzłem na drodze pakietu, to zawsze wpisze w pole swoje klucz:wartość.

- get-min - Identycznie jak dla get-max, z tym, że poszukiwana jest oczywiście wartość najmniejsza.

- new-reocrd - Jedyny rodzaj żądania od klienta, które nie wywołuje w węźle przekazania pakietu do wewnętrznego algorytmu routingu. Węzeł umieszcza w swojej pamięci wewnętrznej otrzymaną klucz:wartość i odsyła do nadawcy-klienta komunikat o treści "OK"

- termiante - Po otrzymaniu od klienta komunikatu o treści "terminate" węzeł przed zamknięciem wysyła do każdego socketu, który posiada, wiadomość "OK", a potem zamyka gniazda. Dzięki temu klient otrzymuje odpowiedź, której oczekuje, a węzły - sąsiedzi po otrzymaniu takiego samego komunikatu "OK" wiedzą, że nadawca zakończył żywot, więc należy zamknąć po swojej stronie jego gniazdo i usunąć je z listy posiadanych połączeń i sąsiadów.


# KOMPILACJA

Zgodnie ze skryptem dostarczonym przez dr Adama Smyka - skrypt compile.bat, o zawartości:

javac *.java
pause

Przy wykorzystaniu Javy 1.8.


# URUCHOMIENIE

W pełni zgodne ze specyfikacją wymagań projektowych.



# CO NIE ZOSTAŁO ZAIMPLEMENTOWANE

n/d, wszystkie wymagania projektowe zostały spełnione, każdy skrypt dostarczony wraz z projektem pomyślnie zakończył działanie na komputerze autora rozwiązania.



# CO NIE DZIAŁA

Autor napotkał problem w postaci adresu pierwszego węzła w bazie. Każdy węzeł w momencie utworzenia połączenia sprawdza swój adres w oparciu o gniazdo, którego używa w połączeniu do innego, istniejącego węzła. Pierwszy węzeł sieci nie tworzy takiego gniazda, dlatego autor "zadrutował" pierwszemu węzłowi na stałe adres 127.0.0.1. Z oczywistych przyczyn może to wykluczyć funkcjonowanie bazy na węzłach uruchomionych na oddzielnych komputerach. Jedyne problemy, jakie to przysporzyło autorowi, dotyczyły sieci węzłów bazy danych o topologii pierścienia. Innych usterek nie stwierdzono, chociaż, w myśl 7-mej zasady testowania:

"Przekonanie o braku błędów jest błędem"
