# Implementation details

## Technology choice

Spring was chosen as a core framework because:
- It facilitates the development. It includes all required dependencies and allows to quckly
write code related to REST API, database access, integration tests and access to some 3d party API.
- It's extremely popular and well-known. So, it makes sense to use it when the code
is intended to be shared with someone else.

H2 db was chosen because it's embedded by default, which allows to easily test the code and 
share it with anyone else. On the other hand, all JPA-related code is totally compatible with any
other db which allows to switch in the future to any other data source.

Test task requirements say nothing about where I should store the phone info. It was easier 
to just store it in memory as a preconfigured collection, but this choice looked unnatural since in 
real-time applications some form of persistence is mandatory. In-memory solution won't work in
case we want to change the list of phones on the fly or even if on server reload we want to preserve info 
about who booked which phone.

Kotlin was chosen because it's the language used by the hiring company. It's a new language 
for me, and it's my first project in it. But I decided that I can write good enough code in it
using Spring and Kotlin official documentation. I was explicitly allowed to use Java, which I 
know much better, but choosing Kotlin was my deliberate choice.

## Things out of scope

I intended to write code as production-ready as possible but there is a lot of things
that I omitted and that should be included in real projects. Things out of the scope 
comprise:
- Any form of security. There is no authentication/authorization/CSRF protection, etc.
- Consequently, there is no user list. When booking a phone, any booker name can be chosen.
- CRUD for phones. If it's necessary to change a list of phones, the import.sql file should
  be edited, or H2 should be accessed.
- Modules. It's common to divide such projects in multiple modules for better class 
  segregation or for the possibility to later run them in separate services. There are 2 obvious ways
  of modularization. 2 modules: REST API (Controllers) + Business logic.
  Or 3 modules: REST API + Business logic + Data layer (accessing db-related classes). Anyway, 
  the current solution is monolith because of its small size.
- There is no documentation on how to use this REST API. Usually, for these purposes Open API
  (Swagger) is used.
- Statistics/Monitoring. Actuator, Pegasus, etc.
- Request logging. In most production systems all requests are logged. At least, in logs are put
endpoints and the time of when the request came and when the response was returned.
Whether params/request body/response body are logged depends on the nature of the project.
How everything is logged, by tomcat itself or by some reverse-proxy (nginx), also varies. Anyway,
this logging should be done automatically for any request, and not by manually calling Spring
logger.

## Things that can be improved in the solution

Aside from things that are not implemented at all, there are things that are implemented
but implemented poorly or there is an obvious room for improvement. The reason they are
in this state is, because the test task requirements clearly state that I should've aimed
to complete the task in 4 hours. Some design solution just cannot be implemented in this 
little time. So, I decided to explicitly list them there just to show that I'm aware
of flaws of my solution. These TODOS include:
- Currently, if the phone is booked, it's written into the PHONES table itself. It's clearly
bad design. There should be a separate table BOOKINGS for this.
- When a list of phones is requested, code calls Phonoapi for more information. It's bad design.
There are 2 much better ways to handle this: (1) request this info from Phonoapi when phones
are added into the system. In this case we also need some mechanism to handle situations
when calls to Phonoapi fail. (2) There can be a separate worker (scheduler) which updates
phone info from time to time. Anyway, requesting this info real-time when our API is used 
is not a good solution.
- PhoneSharingController is under-tested. For example, #bookPhone returns 400 
when bookerName is empty, and it's not tested anywhere. More time is required to write such tests.
- PhoneInfoService is under-tested. There is async logic. I need more time to test it properly.
But I manually tested it using logs/Thread.sleep/delay, so I know it's working as expected.
- Logging of requests to Phonoapi. Contacting 3d party API is often the most fragile part
of the application. Requests/responses are often fully logged (including headers) to later
trace problems. This can be implemented as a separate Spring service.
- When multiple requests simultaneously try to book or return the same phone, the 409 status is
returned. I'm not sure that it's the best solution. It's discussable. Maybe, there are more
graceful ways to handle this, but more time is needed to think about it.

## Conclusion

Things that I find important and things that you find important may not be the same. I spent
some time writing:
- Validation for all possible cases.
- Async logic to send multiple requests to Phonoapi and send them in a way that guarantees 
that we don't request the same phone info twice.
- Some db logic although all of it could've been implemented in memory.
- Integration tests for service methods.

At the same time I decided not to implement some other things that can be perceived as important:
security, automatic request logging, etc.

If you think, that I didn't implement something essential, please inform me.
