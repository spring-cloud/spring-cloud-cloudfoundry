// to run locally use:
//      spring run hi-service.groovy
//
// to deploy to Cloud Foundry use:
//      spring jar hi.jar hi-service.groovy
//      cf push hi-service -p hi.jar


import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class GreetingRestController {

    @RequestMapping("/hi/{name}")
    def hi(@PathVariable String name) {
     [greeting: "Hello, " + name + "!"]
    }
}