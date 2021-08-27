"use strict";

function Page1({openView,setPageTitle}) {
    const [cnt, setCnt] = useState(0)

    useEffect(() => {
        be.add(26).then(res => setCnt(res))
        setCnt(2)
    }, [])

    return RE.Container.col.top.center({style:{marginTop:'200px'}},{},
        `This is Page1 [${cnt}]`,
        RE.Button({variant:"contained", onClick: () => openView(HOME_PAGE_VIEW)}, 'Go to home page'),
    )
}
