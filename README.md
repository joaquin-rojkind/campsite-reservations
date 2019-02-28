# campsite-reservations

## Tech Stack

* Java 8
* Spring Boot
* Spring Web
* Spring Data JPA
* H2
* Maven
* Mockito
* Junit
* Rest Assured


## Build

Build application and run all unit and integration tests:

`mvn clean install`

Skip integration tests:

`mvn clean install -Dskip.it=true`

Skip all tests:

`mvn clean install -DskipTests`

## Run

`mvn spring-boot:run`

Application listens on `http://localhost:8081`. Port can be changed in the `application.yml` file.

## Database

Access the H2 database console at `http://localhost:8081/console`
 
## API

### Postman

In order to execute functional tests two files have been provided in the `src/test/resources` directory: a Postman collection and a Postman environment. 
The collection contains a sample request for each endpoint.   
1. Import the collection into Postman
2. Import the environment into Postman
3. Open the collection and set the environment
4. Replace param values and payload values as needed
5. Test away!

### Endpoints

Get Availability (default date range)

`GET /api/reservations`

---

Get Availability

`GET /api/reservations?startDate=2019-03-22&endDate=2019-03-26`

---

Make Reservation

```
POST /api/reservations

{
	"email": "someone@something.com",
	"fullName": "John Smith",
	"arrivalDate": "2019-03-17",
	"departureDate": "2019-03-18"
}
```

---

Modify Reservation

```
PUT /api/reservations/{id}

{
	"email": "new@new.com",
	"fullName": "Paul Johnson",
	"arrivalDate": "2019-03-24",
	"departureDate": "2019-03-26"
}
```

---

Cancel Reservation

`DELETE /api/reservations/{id}`

---

Read Reservation

`GET /api/reservations/{id}`

## System Requirements

* The campsite can be reserved for max 3 days.
* The campsite can be reserved minimum 1 day(s) ahead of arrival and up to 1 month in advance.
* Check-in & check-out time is 12:00 AM.
* It’s possible to check availability for a given date range with the default being 1 month.
* It’s possible to make reservation providing email and full name along with intended arrival date and departure date. Should return a unique booking identifier.
* It’s possible to modify a reservation.
* It’s possible to cancel reservation.
* System provides appropriate error messages to the caller to indicate the error cases.
* System can gracefully handle concurrent requests to reserve the campsite. 
* System should be able to handle large volume of requests for getting the campsite availability.
* There are no restrictions on how reservations are stored as long as system constraints are not violated.

## Assumptions

* Campsite’s capacity is one reservation at a time.
* Check-in & check-out time is 12:00 AM, this being midnight in either case, so that in a 24 hour clock check-in is at 0:00 of the given arrival date and check-out is at 24:00 of the given departure date. This also means that the end date is inclusive.
* Since reservations are minimum 1 day(s) ahead of arrival and up to 1 month in advance, checking availability also works within the same 30 day time span.
