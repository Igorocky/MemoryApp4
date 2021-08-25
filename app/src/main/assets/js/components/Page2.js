"use strict";

function Page2({openView,setPageTitle}) {
    return RE.Container.col.top.center({style:{marginTop:'200px'}},{},
        'This is Page2',
        RE.Button({variant:"contained", onClick: () => openView(HOME_PAGE_VIEW)}, 'Go to home page'),
    )
}
