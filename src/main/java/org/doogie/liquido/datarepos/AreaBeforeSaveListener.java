package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.AreaModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;

//Currently not used @Configuration
public class AreaBeforeSaveListener extends AbstractMongoEventListener<AreaModel> {
  //@Autowired
  //AreaRepo areaRepo;

  @Override
  public void onBeforeSave(BeforeSaveEvent<AreaModel> event) {
    AreaModel area = event.getSource();
    System.out.println("====> onBeforeSave "+area);
  }
}
