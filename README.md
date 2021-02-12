# GitHub rank

***
### Run
> sbt "run 8080"

### Test
> sbt test

#### Environment variables
- GH_TOKEN - your personal access token

***
### API
#### Organization contributors rank
Returns json array with contributors info
> /org/:org_name/contributors

- **200 OK** Successful result - contributors list
```json
[
  {
    "name": "rozza",
    "contributions": 368
  },
  {
    "name": "glaszig",
    "contributions": 111
  },
  {
    "name": "kazievab",
    "contributions": 95
  },
  {
    "name": "lxcid",
    "contributions": 56
  },
  {
    "name": "lukescott",
    "contributions": 17
  }
]
```

- **404 Not found** Organization not found
- **403 Forbidden** Request limit exceeded