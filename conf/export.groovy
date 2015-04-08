plan(key:'ENTITY',name:'Entity Engine',description:'Atlassian JIRA fork of OfBiz Entity Engine') {   
   project(key:'OFBEE',name:'OfBiz Entity Engine Fork')
   
   repository(name:'entity-engine')
   
   trigger(type:'polling',description:'Poll every 15 minutes',
      strategy:'periodically',frequency:'900') {      
      repository(name:'entity-engine')
      
   }
   notification(type:'Failed Builds and First Successful',
      recipient:'user',user:'cfuller')
   
   stage(name:'Build and Test') {      
      job(key:'JDK16',name:'JDK 1.6') {         
         task(type:'checkout',description:'Checkout Default Repository') {            
            repository(name:'entity-engine')
            
         }
         task(type:'maven3',description:'mvn clean verify',
            goal:'clean verify',mavenExecutable:'Maven 3.2',
            buildJdk:'JDK 1.6',hasTests:'true')
         
      }
      job(key:'JDK18',name:'JDK 1.8') {         
         task(type:'checkout',description:'Checkout Default Repository') {            
            repository(name:'entity-engine')
            
         }
         task(type:'maven3',description:'mvn clean verify',
            goal:'clean verify',mavenExecutable:'Maven 3.2',
            buildJdk:'JDK 1.8',hasTests:'true')
         
      }
      job(key:'JDK17',name:'JDK 1.7') {         
         task(type:'checkout',description:'Checkout Default Repository') {            
            repository(name:'entity-engine')
            
         }
         task(type:'maven3',description:'mvn clean verify',
            goal:'clean verify',mavenExecutable:'Maven 3.2',
            buildJdk:'JDK 1.7',hasTests:'true')
         
      }
   }
   stage(name:'Release',manual:'true') {      
      job(key:'REL',name:'Release') {         
         task(type:'checkout',description:'Checkout Default Repository') {            
            repository(name:'entity-engine')
            
         }
         task(type:'maven3',description:'Release',goal:'-B "-Darguments=-DskipTests" release:prepare release:perform',
            mavenExecutable:'Maven 3.2',buildJdk:'JDK 1.6')
         
      }
   }
   branchMonitoring(enabled:'true',matchingPattern:'issue/.*',timeOfInactivityInDays:'30',
      notificationStrategy:'NOTIFY_COMMITTERS',remoteJiraBranchLinkingEnabled:'true')
   
   dependencies(triggerForBranches:'true')
   
   permissions() {      
      user(name:'cfuller',permissions:'read,write,build,clone,administration')
      
      anonymous(permissions:'read')
      
      loggedInUser(permissions:'read')
      
   }
}
