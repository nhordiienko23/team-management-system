package com.nba.team;

import java.time.LocalDate;

 record TeamSearchFilter(
  String name,
  Integer minChampionshipTitleCount,
  Integer maxChampionshipTitleCount,
  LocalDate creationDateStart,
  LocalDate creationDateEnd
){
}
