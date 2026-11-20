| HTTP check | Expected | Actual | Result |
|---|---:|---:|---|
| reset before run | 200 | 200 | PASS |
| list departments | 200 | 200 | PASS |
| search seed | 200 | 200 | PASS |
| create duplicate and minor accepted by TX1 | 201 | 201 | PASS |
| read created record | 200 | 200 | PASS |
| duplicate search returns both records | 200 | 200 | PASS |
| numeric range filter | 200 | 200 | PASS |
| empty result list | 200 | 200 | PASS |
| double boundary retains TX1 grade | 200 | 200 | PASS |
| other duplicate unchanged | 200 | 200 | PASS |
| NaN accepted and represented as string | 200 | 200 | PASS |
| Infinity pair retains NaN ratio | 200 | 200 | PASS |
| negative days | 422 | 422 | PASS |
| excess days | 422 | 422 | PASS |
| zero standard | 422 | 422 | PASS |
| invalid numeric text | 422 | 422 | PASS |
| invalid update leaves prior data | 200 | 200 | PASS |
| invalid date chronology | 422 | 422 | PASS |
| blank name | 422 | 422 | PASS |
| numeric name is not coerced to text | 400 | 400 | PASS |
| malformed date | 400 | 400 | PASS |
| malformed UUID | 400 | 400 | PASS |
| unknown UUID | 404 | 404 | PASS |
| empty query | 422 | 422 | PASS |
| missing query | 400 | 400 | PASS |
| wrong JSON type | 400 | 400 | PASS |
| raw NaN is invalid JSON | 400 | 400 | PASS |
| unknown JSON field | 400 | 400 | PASS |
| unsupported method | 405 | 405 | PASS |
| wrong content type | 415 | 415 | PASS |
| reset after run | 200 | 200 | PASS |
| old UUID invalid after reset | 404 | 404 | PASS |
